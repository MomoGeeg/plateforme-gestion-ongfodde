package ong.fodde.modules.acquisitions.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.acquisitions.dto.AcquisitionRequest;
import ong.fodde.modules.acquisitions.dto.AcquisitionResponse;
import ong.fodde.modules.acquisitions.entity.Acquisition;
import ong.fodde.modules.acquisitions.entity.StatutAcquisition;
import ong.fodde.modules.acquisitions.repository.AcquisitionRepository;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.historique.entity.TypeDemande;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.shared.securite.AccesPersonnel;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

import static ong.fodde.shared.util.Prechargement.precharger;

@Service
@Transactional
public class AcquisitionService {

    private final AcquisitionRepository acquisitionRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final HistoriqueService historiqueService;
    private final AccesPersonnel accesPersonnel;
    private final PerimetreRepository perimetreRepository;

    public AcquisitionService(AcquisitionRepository acquisitionRepository,
                               UserRepository userRepository,
                               ProjetRepository projetRepository,
                               HistoriqueService historiqueService,
                               AccesPersonnel accesPersonnel,
                               PerimetreRepository perimetreRepository) {
        this.acquisitionRepository = acquisitionRepository;
        this.userRepository = userRepository;
        this.projetRepository = projetRepository;
        this.historiqueService = historiqueService;
        this.accesPersonnel = accesPersonnel;
        this.perimetreRepository = perimetreRepository;
    }

    /** Historique de l'acquisition. Visible par le demandeur et par le RAF, le SE et l'administrateur. */
    @Transactional(readOnly = true)
    public List<EtapeHistorique> historique(UUID id, User user) {
        Acquisition a = getOrThrow(id);
        if (!estCoordonnateurDuProjet(a, user)) {
            accesPersonnel.verifierUtilisateur(a.getSoumisParId(), user, "ROLE_RAF", "ROLE_SE", "ROLE_ADMIN");
        }
        // Acquisitions traitées avant l'historique : la décision est connue, pas son auteur.
        List<EtapeHistorique> anciennes = a.getStatut() == StatutAcquisition.EN_ATTENTE
                || a.getStatut() == StatutAcquisition.ATTENTE_COORDONNATEUR
                ? List.of()
                : List.of(historiqueService.reconstituer(null, a.getStatut(), null, a.getMotifRejet(), null));
        EtapeHistorique creation = historiqueService.reconstituer(null, StatutAcquisition.EN_ATTENTE,
                a.getSoumisParId(), null, a.getCreatedAt());
        return historiqueService.historique(TypeDemande.ACQUISITION, id, creation, anciennes);
    }

    public AcquisitionResponse creerAcquisition(AcquisitionRequest request, UUID soumisParId) {
        Acquisition acq = new Acquisition();
        StatutAcquisition statutInitial = StatutAcquisition.EN_ATTENTE;
        acq.setCodeActif(request.codeActif());
        acq.setDesignation(request.designation());
        acq.setJustification(request.justification());
        acq.setCategorie(request.categorie());
        if (request.perimetreId() != null) {
            Perimetre perimetre = perimetreRepository.findById(request.perimetreId())
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                    .orElseThrow(() -> new ResourceNotFoundException("Périmètre introuvable : " + request.perimetreId()));
            if (request.projetId() != null && !request.projetId().equals(perimetre.getProjetId())) {
                throw new BusinessException("Ce périmètre n'appartient pas au projet choisi");
            }
            acq.setPerimetreId(perimetre.getId());
            acq.setProjetId(perimetre.getProjetId());
        } else if (request.projetId() != null) {
            acq.setProjetId(request.projetId());
        } else {
            throw new BusinessException("Choisissez le projet ou le périmètre concerné");
        }
        // Le coordonnateur du projet (chef de projet) vise l'achat avant le RAF, sauf s'il le demande lui-même.
        UUID coordonnateurId = coordonnateurDuProjet(acq.getProjetId());
        if (coordonnateurId != null && !coordonnateurId.equals(soumisParId)) {
            statutInitial = StatutAcquisition.ATTENTE_COORDONNATEUR;
        }
        acq.setValeur(request.valeur());
        acq.setDateAcquisition(request.dateAcquisition());
        acq.setSoumisParId(soumisParId);
        acq.setStatut(statutInitial);

        Acquisition saved = acquisitionRepository.save(acq);
        historiqueService.enregistrer(TypeDemande.ACQUISITION, saved.getId(), null, statutInitial, soumisParId, null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AcquisitionResponse> listerToutesAcquisitions() {
        return toResponses(acquisitionRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<AcquisitionResponse> mesAcquisitions(UUID userId) {
        return toResponses(acquisitionRepository.findBySoumisParId(userId));
    }

    /** Achats demandés pour les projets que coordonne l'utilisateur (chef de projet). */
    @Transactional(readOnly = true)
    public List<AcquisitionResponse> achatsDeMesProjets(User user) {
        List<UUID> projets = projetRepository.findByChefProjetId(user.getId()).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(p -> p.getId()).toList();
        if (projets.isEmpty()) {
            return List.of();
        }
        return toResponses(acquisitionRepository.findByProjetIdIn(projets).stream()
                .sorted(java.util.Comparator.comparing(Acquisition::getCreatedAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList());
    }

    /** Visa du coordonnateur du projet : l'achat part ensuite au RAF. */
    public AcquisitionResponse viser(UUID id, User user) {
        Acquisition acq = attenteCoordonnateur(id, user);
        acq.setStatut(StatutAcquisition.EN_ATTENTE);
        Acquisition saved = acquisitionRepository.save(acq);
        historiqueService.enregistrer(TypeDemande.ACQUISITION, id, StatutAcquisition.ATTENTE_COORDONNATEUR,
                StatutAcquisition.EN_ATTENTE, user.getId(), null);
        return toResponse(saved);
    }

    /** Refus du coordonnateur, motif obligatoire. */
    public AcquisitionResponse rejeterParCoordonnateur(UUID id, String motif, User user) {
        Acquisition acq = attenteCoordonnateur(id, user);
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        acq.setStatut(StatutAcquisition.REJETEE);
        acq.setMotifRejet(motif);
        Acquisition saved = acquisitionRepository.save(acq);
        historiqueService.enregistrer(TypeDemande.ACQUISITION, id, StatutAcquisition.ATTENTE_COORDONNATEUR,
                StatutAcquisition.REJETEE, user.getId(), motif);
        return toResponse(saved);
    }

    private Acquisition attenteCoordonnateur(UUID id, User user) {
        Acquisition acq = getOrThrow(id);
        if (!estCoordonnateurDuProjet(acq, user) && !accesPersonnel.aUnRole(user, "ROLE_ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Seul le coordonnateur du projet vise cet achat");
        }
        if (acq.getStatut() != StatutAcquisition.ATTENTE_COORDONNATEUR) {
            throw new BusinessException("Cet achat n'attend pas le visa du coordonnateur");
        }
        return acq;
    }

    private UUID coordonnateurDuProjet(UUID projetId) {
        return projetId == null ? null : projetRepository.findById(projetId)
                .map(p -> p.getChefProjetId())
                .orElse(null);
    }

    private boolean estCoordonnateurDuProjet(Acquisition a, User user) {
        return user.getId().equals(coordonnateurDuProjet(a.getProjetId()));
    }

    /** Conversion d'une liste : projets et auteurs chargés en une requête chacun. */
    private List<AcquisitionResponse> toResponses(List<Acquisition> acquisitions) {
        precharger(projetRepository, acquisitions, Acquisition::getProjetId);
        precharger(userRepository, acquisitions, Acquisition::getSoumisParId);
        return acquisitions.stream().map(this::toResponse).toList();
    }

    public AcquisitionResponse validerAcquisition(UUID id, UUID userId) {
        Acquisition acq = getOrThrow(id);
        if (acq.getStatut() != StatutAcquisition.EN_ATTENTE) {
            throw new BusinessException("Seule une acquisition EN_ATTENTE peut être validée (statut : " + acq.getStatut() + ")");
        }
        acq.setStatut(StatutAcquisition.VALIDEE);
        Acquisition saved = acquisitionRepository.save(acq);
        historiqueService.enregistrer(TypeDemande.ACQUISITION, id, StatutAcquisition.EN_ATTENTE, StatutAcquisition.VALIDEE, userId, null);
        return toResponse(saved);
    }

    public AcquisitionResponse rejeterAcquisition(UUID id, String motif, UUID userId) {
        Acquisition acq = getOrThrow(id);
        if (acq.getStatut() != StatutAcquisition.EN_ATTENTE) {
            throw new BusinessException("Seule une acquisition EN_ATTENTE peut être rejetée (statut : " + acq.getStatut() + ")");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        acq.setStatut(StatutAcquisition.REJETEE);
        acq.setMotifRejet(motif);
        Acquisition saved = acquisitionRepository.save(acq);
        historiqueService.enregistrer(TypeDemande.ACQUISITION, id, StatutAcquisition.EN_ATTENTE, StatutAcquisition.REJETEE, userId, motif);
        return toResponse(saved);
    }

    private Acquisition getOrThrow(UUID id) {
        return acquisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acquisition introuvable : " + id));
    }

    private AcquisitionResponse toResponse(Acquisition a) {
        var projet = projetRepository.findById(a.getProjetId());
        String projetCode = projet.map(p -> p.getCode()).orElse(null);

        String soumisPar = a.getSoumisParId() != null
                ? userRepository.findById(a.getSoumisParId())
                        .map(u -> u.getPrenom() + " " + u.getNom())
                        .orElse(null)
                : null;

        return new AcquisitionResponse(
                a.getId(),
                a.getCodeActif(),
                a.getDesignation(),
                a.getJustification(),
                a.getCategorie(),
                a.getProjetId(),
                projetCode,
                a.getValeur(),
                a.getDateAcquisition(),
                a.getSoumisParId(),
                soumisPar,
                a.getStatut(),
                a.getMotifRejet(),
                a.getCreatedAt(),
                a.getPerimetreId(),
                a.getPerimetreId() == null ? null : perimetreRepository.findById(a.getPerimetreId())
                        .map(Perimetre::getNom).orElse(null),
                projet.map(p -> p.getChefProjetId()).orElse(null)
        );
    }
}
