package ong.fodde.modules.rh.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.rh.dto.CompteSansFicheResponse;
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
        verifierSuperieur(null, request.superieurId());
        employe.setPole(request.pole());
        employe.setSuperieurId(request.superieurId());
        employe.setStatut(StatutEmploye.ACTIF);

        Employe saved = employeRepository.save(employe);
        return toResponse(saved);
    }

    /**
     * Modifie la fiche : identité, poste, pôle et supérieur direct. Le matricule ne change pas ;
     * le lien avec le compte se gère par l'administrateur (associer / dissocier) ; le contrat et
     * le statut ont leurs propres routes.
     */
    public EmployeResponse modifierEmploye(UUID id, EmployeRequest request) {
        Employe employe = employeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'id : " + id));
        verifierSuperieur(id, request.superieurId());
        employe.setNom(request.nom());
        employe.setPrenom(request.prenom());
        employe.setDateNaissance(request.dateNaissance());
        employe.setLieuNaissance(request.lieuNaissance());
        employe.setNationalite(request.nationalite());
        employe.setTelephone(request.telephone());
        employe.setAdresse(request.adresse());
        employe.setPoste(request.poste());
        employe.setDepartement(request.departement());
        if (request.dateEmbauche() != null) {
            employe.setDateEmbauche(request.dateEmbauche());
        }
        employe.setPole(request.pole());
        employe.setSuperieurId(request.superieurId());
        return toResponse(employeRepository.save(employe));
    }

    /**
     * Le supérieur direct doit être une autre fiche, active, et la hiérarchie ne doit pas
     * tourner en rond (A dépend de B qui dépend de A).
     */
    void verifierSuperieur(UUID employeId, UUID superieurId) {
        if (superieurId == null) {
            return;
        }
        if (superieurId.equals(employeId)) {
            throw new BusinessException("Une personne ne peut pas être son propre supérieur");
        }
        Employe sup = employeRepository.findById(superieurId)
                .orElseThrow(() -> new ResourceNotFoundException("Supérieur introuvable : " + superieurId));
        if (sup.getStatut() == StatutEmploye.PARTI) {
            throw new BusinessException(sup.getPrenom() + " " + sup.getNom()
                    + " a quitté l'ONG : choisissez un autre supérieur");
        }
        if (employeId == null) {
            return; // nouvelle fiche : personne ne dépend encore d'elle
        }
        Set<UUID> vus = new java.util.HashSet<>();
        UUID courant = sup.getSuperieurId();
        while (courant != null && vus.add(courant)) {
            if (courant.equals(employeId)) {
                throw new BusinessException(sup.getPrenom() + " " + sup.getNom()
                        + " dépend déjà de cette personne : il ne peut pas être son supérieur");
            }
            courant = employeRepository.findById(courant).map(Employe::getSuperieurId).orElse(null);
        }
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

    /**
     * Comptes sans fiche employé (hors comptes désactivés), triés par nom :
     * chaque personne qui a un compte doit avoir sa fiche, créée par les RH.
     */
    @Transactional(readOnly = true)
    public List<CompteSansFicheResponse> listerComptesSansFiche() {
        Set<UUID> lies = employeRepository.findAll().stream()
                .map(Employe::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return userRepository.findAll().stream()
                .filter(u -> !lies.contains(u.getId()))
                .filter(u -> u.getStatut() != StatutUser.INACTIF)
                .sorted(Comparator.comparing((User u) -> u.getNom().toLowerCase())
                        .thenComparing(u -> u.getPrenom().toLowerCase()))
                .map(u -> new CompteSansFicheResponse(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(),
                        u.getTelephone(), u.getService(), u.getStatut(),
                        u.getRoles().stream().map(r -> r.getNom()).sorted().toList()))
                .toList();
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
                e.getPole(),
                e.getSuperieurId(),
                e.getSuperieurId() == null ? null : employeRepository.findById(e.getSuperieurId())
                        .map(sup -> sup.getPrenom() + " " + sup.getNom()).orElse(null),
                e.getCreatedAt(),
                contratActif != null ? contratActif.getTypeContrat() : null,
                contratActif != null ? contratActif.getDateFin() : null,
                e.getSoldeConges()
        );
    }
}
