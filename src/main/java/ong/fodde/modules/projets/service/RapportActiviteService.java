package ong.fodde.modules.projets.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.projets.dto.RapportActiviteRequest;
import ong.fodde.modules.projets.dto.RapportActiviteResponse;
import ong.fodde.modules.projets.entity.RapportActivite;
import ong.fodde.modules.projets.entity.StatutRapport;
import ong.fodde.modules.projets.repository.RapportActiviteRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class RapportActiviteService {

    private static final Logger log = LoggerFactory.getLogger(RapportActiviteService.class);

    private final RapportActiviteRepository rapportActiviteRepository;

    public RapportActiviteService(RapportActiviteRepository rapportActiviteRepository) {
        this.rapportActiviteRepository = rapportActiviteRepository;
    }

    public RapportActiviteResponse soumettreRapport(RapportActiviteRequest request) {
        RapportActivite rapport = new RapportActivite();
        rapport.setActiviteId(request.activiteId());
        rapport.setProjetId(request.projetId());
        rapport.setAuteurId(request.auteurId());
        rapport.setTitre(request.titre());
        rapport.setDescriptionAvancement(request.descriptionAvancement());
        rapport.setProblemesRencontres(request.problemesRencontres());
        rapport.setProchainesEtapes(request.prochainesEtapes());
        rapport.setPeriode(request.periode());
        rapport.setDocumentUrl(request.documentUrl());
        rapport.setStatut(StatutRapport.SOUMIS);

        RapportActivite saved = rapportActiviteRepository.save(rapport);
        log.info("📄 Rapport d'activité soumis : {}", saved.getId());
        return toResponse(saved);
    }

    public RapportActiviteResponse validerRapportCp(UUID id, UUID validateurId) {
        RapportActivite rapport = rapportActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé avec l'id : " + id));

        if (rapport.getStatut() != StatutRapport.SOUMIS) {
            throw new BusinessException("Le rapport doit être au statut SOUMIS pour être validé par le CP");
        }

        rapport.setStatut(StatutRapport.VALIDE_CP);
        rapport.setValideParCp(validateurId);
        rapport.setDateValidationCp(LocalDateTime.now());

        RapportActivite saved = rapportActiviteRepository.save(rapport);
        log.info("✅ Rapport {} validé par le CP ({})", id, validateurId);
        return toResponse(saved);
    }

    public RapportActiviteResponse validerRapportRp(UUID id, UUID validateurId) {
        RapportActivite rapport = rapportActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé avec l'id : " + id));

        if (rapport.getStatut() != StatutRapport.VALIDE_CP) {
            throw new BusinessException("Le rapport doit être au statut VALIDE_CP pour être validé par le RP");
        }

        rapport.setStatut(StatutRapport.VALIDE_RP);
        rapport.setValideParRp(validateurId);
        rapport.setDateValidationRp(LocalDateTime.now());

        RapportActivite saved = rapportActiviteRepository.save(rapport);
        log.info("✅ Rapport {} validé définitivement — archivage GED", id);
        return toResponse(saved);
    }

    public RapportActiviteResponse rejeterRapport(UUID id, UUID validateurId, String motif) {
        RapportActivite rapport = rapportActiviteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé avec l'id : " + id));

        if (rapport.getStatut() != StatutRapport.SOUMIS && rapport.getStatut() != StatutRapport.VALIDE_CP) {
            throw new BusinessException("Le rapport ne peut être rejeté que s'il est au statut SOUMIS ou VALIDE_CP");
        }

        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }

        rapport.setStatut(StatutRapport.REJETE);
        rapport.setMotifRejet(motif);

        RapportActivite saved = rapportActiviteRepository.save(rapport);
        log.info("❌ Rapport {} rejeté par {} — motif : {}", id, validateurId, motif);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RapportActiviteResponse> listerRapportsProjet(UUID projetId) {
        return rapportActiviteRepository.findByProjetId(projetId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RapportActiviteResponse> listerMesRapports(UUID auteurId) {
        return rapportActiviteRepository.findByAuteurId(auteurId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RapportActiviteResponse> listerRapportsEnAttenteCp() {
        return rapportActiviteRepository.findByStatut(StatutRapport.SOUMIS).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RapportActiviteResponse> listerRapportsEnAttenteRp() {
        return rapportActiviteRepository.findByStatut(StatutRapport.VALIDE_CP).stream()
                .map(this::toResponse)
                .toList();
    }

    private RapportActiviteResponse toResponse(RapportActivite r) {
        return new RapportActiviteResponse(
                r.getId(),
                r.getActiviteId(),
                r.getProjetId(),
                r.getAuteurId(),
                r.getTitre(),
                r.getDescriptionAvancement(),
                r.getProblemesRencontres(),
                r.getProchainesEtapes(),
                r.getPeriode(),
                r.getStatut(),
                r.getValideParCp(),
                r.getDateValidationCp(),
                r.getValideParRp(),
                r.getDateValidationRp(),
                r.getMotifRejet(),
                r.getDocumentUrl(),
                r.getCreatedAt()
        );
    }
}
