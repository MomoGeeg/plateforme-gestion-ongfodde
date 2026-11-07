package ong.fodde.modules.projets.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.dto.RapportActiviteRequest;
import ong.fodde.modules.projets.dto.RapportActiviteResponse;
import ong.fodde.modules.projets.service.RapportActiviteService;
import ong.fodde.shared.securite.AccesPersonnel;

@RestController
@RequestMapping("/api/rapports")
public class RapportActiviteController {

    private final RapportActiviteService rapportActiviteService;
    private final AccesPersonnel accesPersonnel;

    public RapportActiviteController(RapportActiviteService rapportActiviteService, AccesPersonnel accesPersonnel) {
        this.rapportActiviteService = rapportActiviteService;
        this.accesPersonnel = accesPersonnel;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RapportActiviteResponse> soumettreRapport(
            @Valid @RequestBody RapportActiviteRequest request) {
        RapportActiviteResponse response = rapportActiviteService.soumettreRapport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RapportActiviteResponse>> listerRapportsProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(rapportActiviteService.listerRapportsProjet(projetId));
    }

    @GetMapping("/mes-rapports/{auteurId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RapportActiviteResponse>> listerMesRapports(@PathVariable UUID auteurId,
                                                                           @AuthenticationPrincipal User currentUser) {
        accesPersonnel.verifierUtilisateur(auteurId, currentUser,
                "ROLE_CHARGE_PROGRAMME", "ROLE_RESPONSABLE_PROGRAMMES", "ROLE_RESPONSABLE_SE", "ROLE_SE", "ROLE_ADMIN");
        return ResponseEntity.ok(rapportActiviteService.listerMesRapports(auteurId));
    }

    @GetMapping("/en-attente-cp")
    @PreAuthorize("hasAuthority('ROLE_CHARGE_PROGRAMME') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<RapportActiviteResponse>> listerRapportsEnAttenteCp() {
        return ResponseEntity.ok(rapportActiviteService.listerRapportsEnAttenteCp());
    }

    @GetMapping("/en-attente-rp")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<RapportActiviteResponse>> listerRapportsEnAttenteRp() {
        return ResponseEntity.ok(rapportActiviteService.listerRapportsEnAttenteRp());
    }

    @PatchMapping("/{id}/valider-cp")
    @PreAuthorize("hasAuthority('ROLE_CHARGE_PROGRAMME') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<RapportActiviteResponse> validerRapportCp(@PathVariable UUID id,
                                                                     @AuthenticationPrincipal User currentUser) {
        UUID validateurId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(rapportActiviteService.validerRapportCp(id, validateurId));
    }

    @PatchMapping("/{id}/valider-rp")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<RapportActiviteResponse> validerRapportRp(@PathVariable UUID id,
                                                                     @AuthenticationPrincipal User currentUser) {
        UUID validateurId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(rapportActiviteService.validerRapportRp(id, validateurId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_CHARGE_PROGRAMME') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<RapportActiviteResponse> rejeterRapport(@PathVariable UUID id,
                                                                   @AuthenticationPrincipal User currentUser,
                                                                   @RequestParam String motif) {
        UUID validateurId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(rapportActiviteService.rejeterRapport(id, validateurId, motif));
    }
}
