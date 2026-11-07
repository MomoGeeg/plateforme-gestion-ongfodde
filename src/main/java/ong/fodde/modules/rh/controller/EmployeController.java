package ong.fodde.modules.rh.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.shared.securite.AccesPersonnel;
import ong.fodde.modules.rh.dto.CompteSansFicheResponse;
import ong.fodde.modules.rh.dto.EmployeRequest;
import ong.fodde.modules.rh.dto.EmployeResponse;
import ong.fodde.modules.rh.entity.StatutEmploye;
import ong.fodde.modules.rh.dto.OrganisationDtos.NoeudOrganigramme;
import ong.fodde.modules.rh.dto.OrganisationDtos.PropositionRattachement;
import ong.fodde.modules.rh.service.EmployeService;
import ong.fodde.modules.rh.service.OrganisationService;

@RestController
@RequestMapping("/api/employes")
public class EmployeController {

    /** Rôles qui voient la liste des employés (mêmes droits que GET /api/employes). */
    private static final String[] ROLES_GESTION = {
        "ROLE_RH", "ROLE_ADMIN", "ROLE_SE", "ROLE_RAF", "ROLE_COMPTABLE", "ROLE_RESPONSABLE_PROGRAMMES"
    };

    private final EmployeService employeService;
    private final OrganisationService organisationService;
    private final AccesPersonnel accesPersonnel;

    public EmployeController(EmployeService employeService, OrganisationService organisationService,
                             AccesPersonnel accesPersonnel) {
        this.employeService = employeService;
        this.organisationService = organisationService;
        this.accesPersonnel = accesPersonnel;
    }

    /** Modifie la fiche (identité, poste, pôle, supérieur direct). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeResponse> modifierEmploye(@PathVariable UUID id,
                                                           @Valid @RequestBody EmployeRequest request) {
        return ResponseEntity.ok(employeService.modifierEmploye(id, request));
    }

    /** Pôle et supérieur proposés d'après les rôles du compte (formulaire RH). */
    @GetMapping("/proposition-rattachement")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<PropositionRattachement> proposerRattachement(@RequestParam UUID userId) {
        return ResponseEntity.ok(organisationService.proposer(userId));
    }

    /** Organigramme : employés actifs avec leur poste, pôle et supérieur (pas de donnée personnelle). */
    @GetMapping("/organigramme")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NoeudOrganigramme>> organigramme() {
        return ResponseEntity.ok(organisationService.organigramme());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeResponse> creerEmploye(@Valid @RequestBody EmployeRequest request) {
        EmployeResponse response = employeService.creerEmploye(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_COMPTABLE') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<List<EmployeResponse>> listerEmployes() {
        return ResponseEntity.ok(employeService.listerEmployes());
    }

    /** Comptes de la plateforme sans fiche employé : les RH créent la fiche, déjà liée au compte. */
    @GetMapping("/comptes-sans-fiche")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CompteSansFicheResponse>> listerComptesSansFiche() {
        return ResponseEntity.ok(employeService.listerComptesSansFiche());
    }

    @GetMapping("/sans-user")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EmployeResponse>> listerEmployesSansCompte() {
        return ResponseEntity.ok(employeService.listerEmployesSansCompte());
    }

    /** Fiche de l'utilisateur connecté (solde de congés…) ; 204 si aucune fiche n'est liée à son compte. */
    @GetMapping("/moi")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeResponse> maFiche(@AuthenticationPrincipal User currentUser) {
        return employeService.maFiche(currentUser.getId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Fiche d'un employé : l'intéressé lui-même ou les rôles qui voient la liste des employés. */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeResponse> obtenirEmploye(@PathVariable UUID id,
                                                          @AuthenticationPrincipal User currentUser) {
        accesPersonnel.verifierEmploye(id, currentUser, ROLES_GESTION);
        return ResponseEntity.ok(employeService.obtenirEmploye(id));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeResponse> changerStatut(@PathVariable UUID id,
                                                          @RequestParam StatutEmploye statut) {
        return ResponseEntity.ok(employeService.changerStatut(id, statut));
    }

    @PatchMapping("/{id}/associer-compte")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeResponse> associerCompte(@PathVariable UUID id,
                                                           @RequestBody Map<String, UUID> body) {
        return ResponseEntity.ok(employeService.associerCompte(id, body.get("userId")));
    }

    @PatchMapping("/{id}/dissocier-compte")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeResponse> dissocierCompte(@PathVariable UUID id) {
        return ResponseEntity.ok(employeService.dissocierCompte(id));
    }
}
