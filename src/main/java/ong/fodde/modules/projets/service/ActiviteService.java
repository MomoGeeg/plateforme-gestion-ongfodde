package ong.fodde.modules.projets.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.projets.dto.ActiviteRequest;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutActivite;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class ActiviteService {

    private final ActiviteRepository activiteRepository;
    private final ProjetRepository projetRepository;

    public ActiviteService(ActiviteRepository activiteRepository, ProjetRepository projetRepository) {
        this.activiteRepository = activiteRepository;
        this.projetRepository = projetRepository;
    }

    public ActiviteResponse creerActivite(ActiviteRequest request) {
        Projet projet = projetRepository.findById(request.projetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Projet non trouvé avec l'id : " + request.projetId()));

        if (projet.getStatut() != StatutProjet.ACTIF) {
            throw new BusinessException("Le projet doit être au statut ACTIF pour y ajouter une activité");
        }

        Activite activite = new Activite();
        activite.setProjetId(request.projetId());
        activite.setTitre(request.titre());
        activite.setDescription(request.description());
        activite.setResponsableId(request.responsableId());
        activite.setParentId(request.parentId());
        activite.setDatePrevuDebut(request.datePrevuDebut());
        activite.setDatePrevuFin(request.datePrevuFin());
        activite.setBudgetAlloue(request.budgetAlloue());
        activite.setStatut(StatutActivite.PLANIFIEE);
        activite.setTauxAvancement(0);

        Activite saved = activiteRepository.save(activite);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ActiviteResponse> listerActivitesProjet(UUID projetId) {
        return activiteRepository.findByProjetId(projetId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActiviteResponse obtenirActivite(UUID id) {
        Activite activite = activiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée avec l'id : " + id));
        return toResponse(activite);
    }

    public ActiviteResponse mettreAJourAvancement(UUID id, Integer taux, StatutActivite statut) {
        Activite activite = activiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée avec l'id : " + id));

        if (taux != null) {
            activite.setTauxAvancement(taux);
        }
        if (statut != null) {
            activite.setStatut(statut);
        }

        Activite saved = activiteRepository.save(activite);
        return toResponse(saved);
    }

    public ActiviteResponse demarrerActivite(UUID id, LocalDate dateReelDebut) {
        Activite activite = activiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activité non trouvée avec l'id : " + id));

        activite.setStatut(StatutActivite.EN_COURS);
        activite.setDateReelDebut(dateReelDebut != null ? dateReelDebut : LocalDate.now());

        Activite saved = activiteRepository.save(activite);
        return toResponse(saved);
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
                a.getCreatedAt()
        );
    }
}
