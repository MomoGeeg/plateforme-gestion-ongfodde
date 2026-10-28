package ong.fodde.modules.projets.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.projets.dto.DemandeActiviteRequest;
import ong.fodde.modules.projets.dto.DemandeActiviteResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.DemandeActivite;
import ong.fodde.modules.projets.entity.StatutActivite;
import ong.fodde.modules.projets.entity.StatutDemandeActivite;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.DemandeActiviteRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class DemandeActiviteService {

    private final DemandeActiviteRepository demandeActiviteRepository;
    private final ActiviteRepository activiteRepository;

    public DemandeActiviteService(DemandeActiviteRepository demandeActiviteRepository,
                                  ActiviteRepository activiteRepository) {
        this.demandeActiviteRepository = demandeActiviteRepository;
        this.activiteRepository = activiteRepository;
    }

    public DemandeActiviteResponse soumettreDemandeActivite(DemandeActiviteRequest request) {
        DemandeActivite demande = new DemandeActivite();
        demande.setProjetId(request.projetId());
        demande.setDemandeurId(request.demandeurId());
        demande.setTitre(request.titre());
        demande.setDescription(request.description());
        demande.setJustification(request.justification());
        demande.setBudgetEstime(request.budgetEstime());
        demande.setDateDebutSouhaitee(request.dateDebutSouhaitee());
        demande.setDateFinSouhaitee(request.dateFinSouhaitee());
        demande.setStatut(StatutDemandeActivite.EN_ATTENTE);

        DemandeActivite saved = demandeActiviteRepository.save(demande);
        return toResponse(saved);
    }

    public DemandeActiviteResponse validerDemandeCp(UUID id, UUID validateurId) {
        DemandeActivite demande = demandeActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée avec l'id : " + id));

        if (demande.getStatut() != StatutDemandeActivite.EN_ATTENTE) {
            throw new BusinessException("La demande doit être au statut EN_ATTENTE pour être validée par le CP");
        }

        demande.setStatut(StatutDemandeActivite.VALIDE_CP);
        demande.setValideParCp(validateurId);
        demande.setDateValidationCp(LocalDateTime.now());

        DemandeActivite saved = demandeActiviteRepository.save(demande);
        return toResponse(saved);
    }

    public DemandeActiviteResponse validerDemandeRp(UUID id, UUID validateurId) {
        DemandeActivite demande = demandeActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée avec l'id : " + id));

        if (demande.getStatut() != StatutDemandeActivite.VALIDE_CP) {
            throw new BusinessException("La demande doit être au statut VALIDE_CP pour être validée par le RP");
        }

        demande.setStatut(StatutDemandeActivite.VALIDE_RP);
        demande.setValideParRp(validateurId);
        demande.setDateValidationRp(LocalDateTime.now());

        DemandeActivite saved = demandeActiviteRepository.save(demande);
        return toResponse(saved);
    }

    public DemandeActiviteResponse approuverDemandeRaf(UUID id, UUID validateurId) {
        DemandeActivite demande = demandeActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée avec l'id : " + id));

        if (demande.getStatut() != StatutDemandeActivite.VALIDE_RP) {
            throw new BusinessException("La demande doit être au statut VALIDE_RP pour être approuvée par le RAF");
        }

        demande.setStatut(StatutDemandeActivite.APPROUVE_RAF);
        demande.setApprouveParRaf(validateurId);
        demande.setDateApprobationRaf(LocalDateTime.now());

        // Créer automatiquement l'activité dans le projet
        Activite activite = new Activite();
        activite.setProjetId(demande.getProjetId());
        activite.setTitre(demande.getTitre());
        activite.setDescription(demande.getDescription());
        activite.setBudgetAlloue(demande.getBudgetEstime());
        activite.setDatePrevuDebut(demande.getDateDebutSouhaitee());
        activite.setDatePrevuFin(demande.getDateFinSouhaitee());
        activite.setStatut(StatutActivite.PLANIFIEE);
        activite.setTauxAvancement(0);

        Activite activiteSaved = activiteRepository.save(activite);
        demande.setActiviteCreeeId(activiteSaved.getId());

        DemandeActivite saved = demandeActiviteRepository.save(demande);
        return toResponse(saved);
    }

    public DemandeActiviteResponse rejeterDemande(UUID id, String motif) {
        DemandeActivite demande = demandeActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande non trouvée avec l'id : " + id));

        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }

        demande.setStatut(StatutDemandeActivite.REJETE);
        demande.setMotifRejet(motif);

        DemandeActivite saved = demandeActiviteRepository.save(demande);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DemandeActiviteResponse> listerDemandesProjet(UUID projetId) {
        return demandeActiviteRepository.findByProjetId(projetId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeActiviteResponse> listerMesDemandes(UUID demandeurId) {
        return demandeActiviteRepository.findByDemandeurId(demandeurId).stream()
                .map(this::toResponse)
                .toList();
    }

    private DemandeActiviteResponse toResponse(DemandeActivite d) {
        return new DemandeActiviteResponse(
                d.getId(),
                d.getProjetId(),
                d.getDemandeurId(),
                d.getTitre(),
                d.getDescription(),
                d.getJustification(),
                d.getBudgetEstime(),
                d.getDateDebutSouhaitee(),
                d.getDateFinSouhaitee(),
                d.getStatut(),
                d.getValideParCp(),
                d.getDateValidationCp(),
                d.getValideParRp(),
                d.getDateValidationRp(),
                d.getApprouveParRaf(),
                d.getDateApprobationRaf(),
                d.getMotifRejet(),
                d.getActiviteCreeeId(),
                d.getCreatedAt()
        );
    }
}
