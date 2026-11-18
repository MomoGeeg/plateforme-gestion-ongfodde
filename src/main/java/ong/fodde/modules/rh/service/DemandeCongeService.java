package ong.fodde.modules.rh.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.historique.entity.TypeDemande;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.rh.dto.DemandeCongeRequest;
import ong.fodde.modules.rh.dto.DemandeCongeResponse;
import ong.fodde.modules.rh.entity.DemandeConge;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.entity.StatutConge;
import ong.fodde.modules.rh.entity.TypeConge;
import ong.fodde.modules.rh.repository.DemandeCongeRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Demandes de congé. Circuit : l'employé demande (EN_ATTENTE) → les RH valident et transmettent
 * (VALIDE_RH) → le RAF approuve (APPROUVE, le solde annuel est alors débité).
 * Les RH peuvent aussi enregistrer un congé pour un employé : il part directement au RAF.
 */
@Service
@Transactional
public class DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;
    private final HistoriqueService historiqueService;

    public DemandeCongeService(DemandeCongeRepository demandeCongeRepository,
                               EmployeRepository employeRepository,
                               HistoriqueService historiqueService) {
        this.demandeCongeRepository = demandeCongeRepository;
        this.employeRepository = employeRepository;
        this.historiqueService = historiqueService;
    }

    private static boolean aUnRole(User user, String... roles) {
        return user.getRoles().stream().anyMatch(r -> List.of(roles).contains(r.getNom()));
    }

    /** Employé concerné par la demande (le contrôle d'accès se fait dans le contrôleur). */
    @Transactional(readOnly = true)
    public UUID employeDeLaDemande(UUID id) {
        return getOrThrow(id).getEmployeId();
    }

    /** Historique de la demande de congé : demande, validation des RH, décision du RAF ou annulation. */
    @Transactional(readOnly = true)
    public List<EtapeHistorique> historique(UUID id) {
        DemandeConge d = getOrThrow(id);
        // Demandes traitées avant l'historique : l'approbation (qui, quand) est connue ; le rejet, sans auteur.
        List<EtapeHistorique> anciennes = switch (d.getStatut()) {
            case APPROUVE -> List.of(historiqueService.reconstituer(StatutConge.EN_ATTENTE, StatutConge.APPROUVE,
                    d.getValideParRaf(), null, d.getDateValidationRaf()));
            case EN_ATTENTE, VALIDE_RH -> List.of();
            default -> List.of(historiqueService.reconstituer(StatutConge.EN_ATTENTE, d.getStatut(),
                    null, d.getMotifRejet(), null));
        };
        EtapeHistorique creation = historiqueService.reconstituer(null, StatutConge.EN_ATTENTE, null, null, d.getCreatedAt());
        return historiqueService.historique(TypeDemande.CONGE, id, creation, anciennes);
    }

    private DemandeConge getOrThrow(UUID id) {
        return demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée avec l'id : " + id));
    }

    /** Nombre de jours d'un congé, premier et dernier jour compris (du 3 au 5 = 3 jours). */
    static int nombreDeJours(java.time.LocalDate debut, java.time.LocalDate fin) {
        return (int) ChronoUnit.DAYS.between(debut, fin) + 1;
    }

    /**
     * Nouvelle demande. Sans employé indiqué (ou pour soi-même), la personne connectée demande
     * pour elle : la demande part aux RH. Les RH peuvent enregistrer un congé pour un employé :
     * il est alors déjà validé par les RH et part au RAF.
     */
    public DemandeCongeResponse soumettreDemande(DemandeCongeRequest request, User auteur) {
        Employe moi = employeRepository.findByUserId(auteur.getId()).orElse(null);
        boolean pourUnAutre = request.employeId() != null
                && (moi == null || !moi.getId().equals(request.employeId()));

        Employe employe;
        if (pourUnAutre) {
            if (!aUnRole(auteur, "ROLE_RH", "ROLE_ADMIN")) {
                throw new AccessDeniedException("Seuls les RH enregistrent un congé pour un autre employé");
            }
            employe = employeRepository.findById(request.employeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employé non trouvé avec l'id : " + request.employeId()));
        } else if (moi != null) {
            employe = moi;
        } else {
            throw new BusinessException("Votre compte n'est lié à aucune fiche employé : adressez-vous aux RH.");
        }

        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("La date de fin est avant la date de début");
        }
        int nbJours = nombreDeJours(request.dateDebut(), request.dateFin());

        if (request.typeConge() == TypeConge.ANNUEL) {
            Integer solde = employe.getSoldeConges() != null ? employe.getSoldeConges() : 0;
            if (solde < nbJours) {
                throw new BusinessException("Solde de congés insuffisant (solde : " + solde
                        + ", demandé : " + nbJours + ")");
            }
        }

        DemandeConge demande = new DemandeConge();
        demande.setEmployeId(employe.getId());
        demande.setTypeConge(request.typeConge());
        demande.setDateDebut(request.dateDebut());
        demande.setDateFin(request.dateFin());
        demande.setMotif(request.motif());
        demande.setJustificatifUrl(request.justificatifUrl());
        demande.setNbJours(nbJours);
        if (pourUnAutre) {
            demande.setStatut(StatutConge.VALIDE_RH);
            demande.setValideParRh(auteur.getId());
            demande.setDateValidationRh(LocalDateTime.now());
        } else {
            demande.setStatut(StatutConge.EN_ATTENTE);
        }

        DemandeConge saved = demandeCongeRepository.save(demande);
        historiqueService.enregistrer(TypeDemande.CONGE, saved.getId(), null, saved.getStatut(), auteur.getId(), null);
        return toResponse(saved);
    }

    /** Les RH valident la demande et la transmettent au RAF. */
    public DemandeCongeResponse validerRH(UUID id, UUID rhId) {
        DemandeConge demande = getOrThrow(id);
        if (demande.getStatut() != StatutConge.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente des RH peuvent être validées");
        }
        demande.setStatut(StatutConge.VALIDE_RH);
        demande.setValideParRh(rhId);
        demande.setDateValidationRh(LocalDateTime.now());
        demandeCongeRepository.save(demande);
        historiqueService.enregistrer(TypeDemande.CONGE, id, StatutConge.EN_ATTENTE, StatutConge.VALIDE_RH, rhId, null);
        return toResponse(demande);
    }

    /** Le RAF approuve après les RH ; le solde annuel est débité. */
    public DemandeCongeResponse approuverRAF(UUID id, UUID validateurId) {
        DemandeConge demande = getOrThrow(id);
        if (demande.getStatut() != StatutConge.VALIDE_RH) {
            throw new BusinessException("Le RAF approuve une demande après la validation des RH");
        }

        demande.setStatut(StatutConge.APPROUVE);
        demande.setValideParRaf(validateurId);
        demande.setDateValidationRaf(LocalDateTime.now());

        if (demande.getTypeConge() == TypeConge.ANNUEL && demande.getNbJours() != null) {
            Employe employe = employeRepository.findById(demande.getEmployeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employé non trouvé avec l'id : " + demande.getEmployeId()));
            Integer soldeActuel = employe.getSoldeConges() != null ? employe.getSoldeConges() : 0;
            employe.setSoldeConges(soldeActuel - demande.getNbJours());
            employeRepository.save(employe);
        }

        demandeCongeRepository.save(demande);
        historiqueService.enregistrer(TypeDemande.CONGE, id, StatutConge.VALIDE_RH, StatutConge.APPROUVE, validateurId, null);
        return toResponse(demande);
    }

    /** Rejet, motif obligatoire : par les RH tant que la demande est chez eux, par le RAF ensuite. */
    public DemandeCongeResponse rejeter(UUID id, User auteur, String motif) {
        DemandeConge demande = getOrThrow(id);
        StatutConge avant = demande.getStatut();
        boolean autorise = switch (avant) {
            case EN_ATTENTE -> aUnRole(auteur, "ROLE_RH", "ROLE_ADMIN");
            case VALIDE_RH -> aUnRole(auteur, "ROLE_RAF", "ROLE_ADMIN");
            default -> throw new BusinessException("Cette demande n'attend plus de décision");
        };
        if (!autorise) {
            throw new AccessDeniedException(avant == StatutConge.EN_ATTENTE
                    ? "La demande est chez les RH" : "La demande est chez le RAF");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Indiquez le motif du rejet");
        }

        demande.setStatut(StatutConge.REJETE);
        demande.setMotifRejet(motif.trim());
        demandeCongeRepository.save(demande);
        historiqueService.enregistrer(TypeDemande.CONGE, id, avant, StatutConge.REJETE, auteur.getId(), motif.trim());
        return toResponse(demande);
    }

    /** Annulation par l'employé lui-même ou par les RH, tant que le RAF n'a pas décidé. */
    public DemandeCongeResponse annuler(UUID id, User auteur) {
        DemandeConge demande = getOrThrow(id);
        StatutConge avant = demande.getStatut();
        if (avant != StatutConge.EN_ATTENTE && avant != StatutConge.VALIDE_RH) {
            throw new BusinessException("Seules les demandes en cours de validation peuvent être annulées");
        }
        boolean luiMeme = employeRepository.findByUserId(auteur.getId())
                .map(e -> e.getId().equals(demande.getEmployeId()))
                .orElse(false);
        if (!luiMeme && !aUnRole(auteur, "ROLE_RH", "ROLE_ADMIN")) {
            throw new AccessDeniedException("Seuls l'employé concerné ou les RH peuvent annuler cette demande");
        }

        demande.setStatut(StatutConge.ANNULE);
        demandeCongeRepository.save(demande);
        historiqueService.enregistrer(TypeDemande.CONGE, id, avant, StatutConge.ANNULE, auteur.getId(), null);
        return toResponse(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> mesConges(UUID employeId) {
        return demandeCongeRepository.findByEmployeId(employeId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Congés de la personne dont la fiche employé est liée à ce compte ; liste vide sinon. */
    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> mesCongesUtilisateur(UUID userId) {
        return employeRepository.findByUserId(userId)
                .map(e -> mesConges(e.getId()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> listerTous() {
        return demandeCongeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Demandes en cours de validation : chez les RH ou chez le RAF. */
    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> congesEnAttente() {
        return Stream.concat(
                        demandeCongeRepository.findByStatut(StatutConge.EN_ATTENTE).stream(),
                        demandeCongeRepository.findByStatut(StatutConge.VALIDE_RH).stream())
                .map(this::toResponse)
                .toList();
    }

    private DemandeCongeResponse toResponse(DemandeConge d) {
        return new DemandeCongeResponse(
                d.getId(),
                d.getEmployeId(),
                d.getTypeConge(),
                d.getDateDebut(),
                d.getDateFin(),
                d.getMotif(),
                d.getStatut(),
                d.getValideParRaf(),
                d.getDateValidationRaf(),
                d.getJustificatifUrl(),
                d.getMotifRejet(),
                d.getNbJours(),
                d.getCreatedAt(),
                d.getValideParRh(),
                d.getDateValidationRh()
        );
    }
}
