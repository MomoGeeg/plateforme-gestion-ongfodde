package ong.fodde.modules.rh.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.rh.dto.EmployeRequest;
import ong.fodde.modules.rh.dto.EmployeResponse;
import ong.fodde.modules.rh.entity.Contrat;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.entity.StatutContrat;
import ong.fodde.modules.rh.entity.StatutEmploye;
import ong.fodde.modules.rh.repository.ContratRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class EmployeService {

    private final EmployeRepository employeRepository;
    private final ContratRepository contratRepository;

    public EmployeService(EmployeRepository employeRepository, ContratRepository contratRepository) {
        this.employeRepository = employeRepository;
        this.contratRepository = contratRepository;
    }

    public EmployeResponse creerEmploye(EmployeRequest request) {
        if (employeRepository.existsByMatricule(request.matricule())) {
            throw new BusinessException("Un employé avec le matricule '" + request.matricule() + "' existe déjà");
        }

        Employe employe = new Employe();
        employe.setMatricule(request.matricule());
        employe.setUserId(request.userId());
        employe.setNom(request.nom());
        employe.setPrenom(request.prenom());
        employe.setDateNaissance(request.dateNaissance());
        employe.setLieuNaissance(request.lieuNaissance());
        employe.setNationalite(request.nationalite());
        employe.setTelephone(request.telephone());
        employe.setAdresse(request.adresse());
        employe.setPoste(request.poste());
        employe.setDepartement(request.departement());
        employe.setDateEmbauche(request.dateEmbauche());
        employe.setSuperviseurId(request.superviseurId());
        employe.setStatut(StatutEmploye.ACTIF);

        Employe saved = employeRepository.save(employe);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeResponse> listerEmployes() {
        return employeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EmployeResponse obtenirEmploye(UUID id) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'id : " + id));
        return toResponse(employe);
    }

    @Transactional(readOnly = true)
    public EmployeResponse obtenirEmployeParUserId(UUID userId) {
        Employe employe = employeRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec le userId : " + userId));
        return toResponse(employe);
    }

    @Transactional(readOnly = true)
    public List<EmployeResponse> listerEmployesSansCompte() {
        return employeRepository.findAll().stream()
                .filter(e -> e.getUserId() == null)
                .map(this::toResponse)
                .toList();
    }

    public EmployeResponse associerCompte(UUID id, UUID userId) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'id : " + id));
        employe.setUserId(userId);
        return toResponse(employeRepository.save(employe));
    }

    public EmployeResponse changerStatut(UUID id, StatutEmploye statut) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'id : " + id));
        employe.setStatut(statut);
        Employe saved = employeRepository.save(employe);
        return toResponse(saved);
    }

    private EmployeResponse toResponse(Employe e) {
        Contrat contratActif = contratRepository
                .findByEmployeIdAndStatut(e.getId(), StatutContrat.ACTIF)
                .orElse(null);

        return new EmployeResponse(
                e.getId(),
                e.getMatricule(),
                e.getUserId(),
                e.getNom(),
                e.getPrenom(),
                e.getDateNaissance(),
                e.getLieuNaissance(),
                e.getNationalite(),
                e.getTelephone(),
                e.getAdresse(),
                e.getPoste(),
                e.getDepartement(),
                e.getDateEmbauche(),
                e.getStatut(),
                e.getSuperviseurId(),
                e.getCreatedAt(),
                contratActif != null ? contratActif.getTypeContrat() : null,
                contratActif != null ? contratActif.getDateFin() : null,
                e.getSoldeConges()
        );
    }
}
