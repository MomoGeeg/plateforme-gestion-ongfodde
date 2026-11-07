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

    public AcquisitionService(AcquisitionRepository acquisitionRepository,
                               UserRepository userRepository,
                               ProjetRepository projetRepository,
                               HistoriqueService historiqueService,
                               AccesPersonnel accesPersonnel) {
        this.acquisitionRepository = acquisitionRepository;
        this.userRepository = userRepository;
        this.projetRepository = projetRepository;
        this.historiqueService = historiqueService;
        this.accesPersonnel = accesPersonnel;
    }

    /** Historique de l'acquisition. Visible par le demandeur et par le RAF, le SE et l'administrateur. */
    @Transactional(readOnly = true)
    public List<EtapeHistorique> historique(UUID id, User user) {
        Acquisition a = getOrThrow(id);
        accesPersonnel.verifierUtilisateur(a.getSoumisParId(), user, "ROLE_RAF", "ROLE_SE", "ROLE_ADMIN");
        // Acquisitions traitées avant l'historique : la décision est connue, pas son auteur.
        List<EtapeHistorique> anciennes = a.getStatut() == StatutAcquisition.EN_ATTENTE
                ? List.of()
                : List.of(historiqueService.reconstituer(null, a.getStatut(), null, a.getMotifRejet(), null));
        EtapeHistorique creation = historiqueService.reconstituer(null, StatutAcquisition.EN_ATTENTE,
                a.getSoumisParId(), null, a.getCreatedAt());
        return historiqueService.historique(TypeDemande.ACQUISITION, id, creation, anciennes);
    }

    public AcquisitionResponse creerAcquisition(AcquisitionRequest request, UUID soumisParId) {
        Acquisition acq = new Acquisition();
        acq.setCodeActif(request.codeActif());
        acq.setDesignation(request.designation());
        acq.setJustification(request.justification());
        acq.setCategorie(request.categorie());
        acq.setProjetId(request.projetId());
        acq.setValeur(request.valeur());
        acq.setDateAcquisition(request.dateAcquisition());
        acq.setSoumisParId(soumisParId);
        acq.setStatut(StatutAcquisition.EN_ATTENTE);

        Acquisition saved = acquisitionRepository.save(acq);
        historiqueService.enregistrer(TypeDemande.ACQUISITION, saved.getId(), null, StatutAcquisition.EN_ATTENTE, soumisParId, null);
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
        String projetCode = projetRepository.findById(a.getProjetId())
                .map(p -> p.getCode())
                .orElse(null);

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
                a.getCreatedAt()
        );
    }
}
