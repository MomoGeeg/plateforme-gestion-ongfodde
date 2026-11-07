package ong.fodde.modules.rh.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
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
    private final UserRepository userRepository;

    public EmployeService(EmployeRepository employeRepository, ContratRepository contratRepository,
                          UserRepository userRepository) {
        this.employeRepository = employeRepository;
        this.contratRepository = contratRepository;
        this.userRepository = userRepository;
    }

    public EmployeResponse creerEmploye(EmployeRequest request) {
        if (employeRepository.existsByMatricule(request.matricule())) {
            throw new BusinessException("Un employé avec le matricule '" + request.matricule() + "' existe déjà");
        }
        if (request.userId() != null) {
            verifierCompteLibre(request.userId(), null);
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

    /** Fiche employé liée au compte, si l'administrateur l'a associée. */
    @Transactional(readOnly = true)
    public Optional<EmployeResponse> maFiche(UUID userId) {
        return employeRepository.findByUserId(userId).map(this::toResponse);
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

    /**
     * Lie une fiche employé à un compte : la personne voit alors ses congés et son solde.
     * Un compte ne peut être lié qu'à une fiche, et une fiche qu'à un compte.
     */
    public EmployeResponse associerCompte(UUID id, UUID userId) {
        if (userId == null) {
            throw new BusinessException("Choisissez le compte à associer");
        }
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'id : " + id));
        if (userId.equals(employe.getUserId())) {
            return toResponse(employe);
        }
        if (employe.getUserId() != null) {
            throw new BusinessException("Cette fiche est déjà liée à un autre compte : dissociez-la d'abord");
        }
        verifierCompteLibre(userId, id);
        employe.setUserId(userId);
        return toResponse(employeRepository.save(employe));
    }

    /** Retire le lien entre la fiche et son compte (erreur d'association, départ…). */
    public EmployeResponse dissocierCompte(UUID id) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'id : " + id));
        employe.setUserId(null);
        return toResponse(employeRepository.save(employe));
    }

    /** Le compte doit exister et ne pas être déjà lié à une autre fiche. */
    private void verifierCompteLibre(UUID userId, UUID employeId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Compte utilisateur introuvable : " + userId);
        }
        employeRepository.findByUserId(userId)
                .filter(autre -> !autre.getId().equals(employeId))
                .ifPresent(autre -> {
                    throw new BusinessException("Ce compte est déjà lié à la fiche de "
                            + autre.getPrenom() + " " + autre.getNom());
                });
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
