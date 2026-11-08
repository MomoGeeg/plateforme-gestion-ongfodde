package ong.fodde.modules.projets.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.ActiviteRequest;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutActivite;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.ResultatAttenduRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Activités d'un projet. Au montage (brouillon), le RP et le chef de projet les planifient sous
 * les résultats attendus ; un projet actif peut encore en recevoir. L'exécution (démarrage,
 * avancement) viendra avec la phase suivante.
 */
@Service
@Transactional
public class ActiviteService {

    private final ActiviteRepository activiteRepository;
    private final ProjetRepository projetRepository;
    private final ResultatAttenduRepository resultatRepository;
    private final UserRepository userRepository;
    private final DroitsProjet droits;

    public ActiviteService(ActiviteRepository activiteRepository, ProjetRepository projetRepository,
                           ResultatAttenduRepository resultatRepository, UserRepository userRepository,
                           DroitsProjet droits) {
        this.activiteRepository = activiteRepository;
        this.projetRepository = projetRepository;
        this.resultatRepository = resultatRepository;
        this.userRepository = userRepository;
        this.droits = droits;
    }

    public ActiviteResponse creerActivite(ActiviteRequest request, User user) {
        Projet projet = projet(request.projetId());
        verifierPlanification(projet, user);
        Activite activite = new Activite();
        activite.setProjetId(projet.getId());
        activite.setStatut(StatutActivite.PLANIFIEE);
        activite.setTauxAvancement(0);
        remplir(activite, projet, request);
        return toResponse(activiteRepository.save(activite));
    }

    /** Une activité se modifie tant qu'elle n'a pas démarré. */
    public ActiviteResponse modifierActivite(UUID id, ActiviteRequest request, User user) {
        Activite activite = activite(id);
        Projet projet = projet(activite.getProjetId());
        verifierPlanification(projet, user);
        if (activite.getStatut() != StatutActivite.PLANIFIEE) {
            throw new BusinessException("Une activité démarrée ne se replanifie pas ici");
        }
        remplir(activite, projet, request);
        return toResponse(activiteRepository.save(activite));
    }

    /** Suppression au montage seulement. */
    public void supprimerActivite(UUID id, User user) {
        Activite activite = activite(id);
        droits.verifierMontage(projet(activite.getProjetId()), user);
        activiteRepository.delete(activite);
    }

    private void verifierPlanification(Projet projet, User user) {
        if (projet.getStatut() == StatutProjet.BROUILLON) {
            droits.verifierMontage(projet, user);
            return;
        }
        if (projet.getStatut() != StatutProjet.ACTIF) {
            throw new BusinessException("Les activités se planifient au montage ou sur un projet actif");
        }
        if (!droits.estGestionnaire(projet, user)) {
            throw new AccessDeniedException("Seuls le RP et le chef de projet planifient les activités");
        }
    }

    private void remplir(Activite a, Projet projet, ActiviteRequest r) {
        if (r.datePrevuFin().isBefore(r.datePrevuDebut())) {
            throw new BusinessException("La fin de l'activité doit suivre son début");
        }
        if (r.datePrevuDebut().isBefore(projet.getDateDebut()) || r.datePrevuFin().isAfter(projet.getDateFin())) {
            throw new BusinessException("L'activité doit se dérouler pendant le projet (du "
                    + projet.getDateDebut() + " au " + projet.getDateFin() + ")");
        }
        if (r.resultatId() != null && resultatRepository.findById(r.resultatId())
                .filter(res -> res.getProjetId().equals(projet.getId())).isEmpty()) {
            throw new BusinessException("Ce résultat n'appartient pas au projet");
        }
        if (r.responsableId() != null && !userRepository.existsById(r.responsableId())) {
            throw new ResourceNotFoundException("Responsable introuvable : " + r.responsableId());
        }
        if (r.budgetAlloue() != null && r.budgetAlloue().signum() < 0) {
            throw new BusinessException("Le budget de l'activité ne peut pas être négatif");
        }
        a.setTitre(r.titre().trim());
        a.setDescription(r.description());
        a.setResultatId(r.resultatId());
        a.setParentId(r.parentId());
        a.setResponsableId(r.responsableId());
        a.setDatePrevuDebut(r.datePrevuDebut());
        a.setDatePrevuFin(r.datePrevuFin());
        a.setBudgetAlloue(r.budgetAlloue());
    }

    @Transactional(readOnly = true)
    public List<ActiviteResponse> listerActivitesProjet(UUID projetId) {
        return activiteRepository.findByProjetId(projetId).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getIsDeleted()))
                .sorted(Comparator.comparing(Activite::getDatePrevuDebut))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActiviteResponse obtenirActivite(UUID id) {
        return toResponse(activite(id));
    }

    public ActiviteResponse mettreAJourAvancement(UUID id, Integer taux, StatutActivite statut) {
        Activite activite = activite(id);
        if (taux != null) {
            if (taux < 0 || taux > 100) {
                throw new BusinessException("L'avancement est un pourcentage entre 0 et 100");
            }
            activite.setTauxAvancement(taux);
        }
        if (statut != null) {
            activite.setStatut(statut);
        }
        return toResponse(activiteRepository.save(activite));
    }

    public ActiviteResponse demarrerActivite(UUID id, LocalDate dateReelDebut) {
        Activite activite = activite(id);
        activite.setStatut(StatutActivite.EN_COURS);
        activite.setDateReelDebut(dateReelDebut != null ? dateReelDebut : LocalDate.now());
        return toResponse(activiteRepository.save(activite));
    }

    private Activite activite(UUID id) {
        return activiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activité introuvable : " + id));
    }

    private Projet projet(UUID id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + id));
    }

    private ActiviteResponse toResponse(Activite a) {
        return new ActiviteResponse(
                a.getId(),
                a.getProjetId(),
                a.getTitre(),
                a.getDescription(),
                a.getResponsableId(),
                a.getParentId(),
                a.getDatePrevuDebut(),
                a.getDatePrevuFin(),
                a.getDateReelDebut(),
                a.getDateReelFin(),
                a.getBudgetAlloue(),
                a.getStatut(),
                a.getTauxAvancement(),
                a.getCreatedAt(),
                a.getResultatId(),
                a.getResponsableId() == null ? null : userRepository.findById(a.getResponsableId())
                        .map(u -> u.getPrenom() + " " + u.getNom()).orElse(null));
    }
}
