package ong.fodde.modules.rh.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@Transactional
public class DemandeCongeService {

    private final DemandeCongeRepository demandeCongeRepository;
    private final EmployeRepository employeRepository;

    public DemandeCongeService(DemandeCongeRepository demandeCongeRepository,
                               EmployeRepository employeRepository) {
        this.demandeCongeRepository = demandeCongeRepository;
        this.employeRepository = employeRepository;
    }

    public DemandeCongeResponse soumettreDemande(DemandeCongeRequest request) {
        Employe employe = employeRepository.findById(request.employeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé non trouvé avec l'id : " + request.employeId()));

        int nbJours = (int) ChronoUnit.DAYS.between(request.dateDebut(), request.dateFin());

        if (request.typeConge() == TypeConge.ANNUEL) {
            Integer solde = employe.getSoldeConges() != null ? employe.getSoldeConges() : 0;
            if (solde < nbJours) {
                throw new BusinessException("Solde de congés insuffisant (solde : " + solde
                        + ", demandé : " + nbJours + ")");
            }
        }

        DemandeConge demande = new DemandeConge();
        demande.setEmployeId(request.employeId());
        demande.setTypeConge(request.typeConge());
        demande.setDateDebut(request.dateDebut());
        demande.setDateFin(request.dateFin());
        demande.setMotif(request.motif());
        demande.setJustificatifUrl(request.justificatifUrl());
        demande.setStatut(StatutConge.EN_ATTENTE);
        demande.setNbJours(nbJours);

        DemandeConge saved = demandeCongeRepository.save(demande);
        return toResponse(saved);
    }

    public DemandeCongeResponse approuverRAF(UUID id, UUID validateurId) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée avec l'id : " + id));

        if (demande.getStatut() != StatutConge.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent être approuvées");
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
        return toResponse(demande);
    }

    public DemandeCongeResponse rejeter(UUID id, UUID validateurId, String motif) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée avec l'id : " + id));

        if (demande.getStatut() != StatutConge.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent être rejetées");
        }

        demande.setStatut(StatutConge.REJETE);
        demande.setMotifRejet(motif);

        demandeCongeRepository.save(demande);
        return toResponse(demande);
    }

    public DemandeCongeResponse annuler(UUID id) {
        DemandeConge demande = demandeCongeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé non trouvée avec l'id : " + id));

        if (demande.getStatut() != StatutConge.EN_ATTENTE) {
            throw new BusinessException("Seules les demandes en attente peuvent être annulées");
        }

        demande.setStatut(StatutConge.ANNULE);

        demandeCongeRepository.save(demande);
        return toResponse(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> mesConges(UUID employeId) {
        return demandeCongeRepository.findByEmployeId(employeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DemandeCongeResponse> listerTous() {
        return demandeCongeRepository.findAll().stream()
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
                d.getCreatedAt()
        );
    }
}
