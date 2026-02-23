package ong.fodde.modules.rh.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.rh.dto.DemandeCongeRequest;
import ong.fodde.modules.rh.dto.DemandeCongeResponse;
import ong.fodde.modules.rh.entity.DemandeConge;
import ong.fodde.modules.rh.entity.StatutConge;
import ong.fodde.modules.rh.repository.DemandeCongeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;

    public DemandeCongeService(DemandeCongeRepository demandeCongeRepository) {
        this.demandeCongeRepository = demandeCongeRepository;
    }

    public DemandeCongeResponse soumettreDemande(DemandeCongeRequest request) {
        DemandeConge demande = new DemandeConge();
        demande.setEmployeId(request.employeId());
        demande.setTypeConge(request.typeConge());
        demande.setDateDebut(request.dateDebut());
        demande.setDateFin(request.dateFin());
        demande.setMotif(request.motif());
        demande.setStatut(StatutConge.EN_ATTENTE);

        DemandeConge saved = demandeCongeRepository.save(demande);
        return toResponse(saved);
    }

    public DemandeCongeResponse traiterDemande(UUID id, StatutConge statut, UUID approbateurId, String commentaire) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée avec l'id : " + id));

        if (demande.getStatut() != StatutConge.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent être traitées");
        }

        if (statut != StatutConge.APPROUVE && statut != StatutConge.REFUSE) {
            throw new BusinessException("Le statut doit être APPROUVE ou REFUSE");
        }

        demande.setStatut(statut);
        demande.setApprobateurId(approbateurId);
        demande.setCommentaireApprobateur(commentaire);

        DemandeConge saved = demandeCongeRepository.save(demande);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> mesConges(UUID employeId) {
        return demandeCongeRepository.findByEmployeId(employeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> congesEnAttente() {
        return demandeCongeRepository.findByStatut(StatutConge.EN_ATTENTE).stream()
                .map(this::toResponse)
                .toList();
    }

    private DemandeCongeResponse toResponse(DemandeConge d) {
        Long nombreJours = (d.getDateDebut() != null && d.getDateFin() != null)
                ? ChronoUnit.DAYS.between(d.getDateDebut(), d.getDateFin())
                : null;

        return new DemandeCongeResponse(
                d.getId(),
                d.getEmployeId(),
                d.getTypeConge(),
                d.getDateDebut(),
                d.getDateFin(),
                d.getMotif(),
                d.getStatut(),
                d.getApprobateurId(),
                d.getCommentaireApprobateur(),
                d.getCreatedAt(),
                nombreJours
        );
    }
}
