package ong.fodde.modules.projets.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.projets.dto.DemandeActiviteRequest;
import ong.fodde.modules.projets.dto.DemandeActiviteResponse;
import ong.fodde.modules.projets.service.DemandeActiviteService;

@RestController
@RequestMapping("/api/demandes-activite")
public class DemandeActiviteController {

    private final DemandeActiviteService demandeActiviteService;

    public DemandeActiviteController(DemandeActiviteService demandeActiviteService) {
        this.demandeActiviteService = demandeActiviteService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandeActiviteResponse> soumettreDemandeActivite(
            @Valid @RequestBody DemandeActiviteRequest request) {
        DemandeActiviteResponse response = demandeActiviteService.soumettreDemandeActivite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DemandeActiviteResponse>> listerDemandesProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(demandeActiviteService.listerDemandesProjet(projetId));
    }

    @GetMapping("/mes-demandes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DemandeActiviteResponse>> listerMesDemandes(@PathVariable UUID id) {
        return ResponseEntity.ok(demandeActiviteService.listerMesDemandes(id));
    }

    @PatchMapping("/{id}/valider-cp")
    @PreAuthorize("hasAuthority('ROLE_CHARGE_PROGRAMME') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeActiviteResponse> validerDemandeCp(@PathVariable UUID id,
                                                                     @RequestParam UUID validateurId) {
        return ResponseEntity.ok(demandeActiviteService.validerDemandeCp(id, validateurId));
    }

    @PatchMapping("/{id}/valider-rp")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeActiviteResponse> validerDemandeRp(@PathVariable UUID id,
                                                                     @RequestParam UUID validateurId) {
        return ResponseEntity.ok(demandeActiviteService.validerDemandeRp(id, validateurId));
    }

    @PatchMapping("/{id}/approuver-raf")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeActiviteResponse> approuverDemandeRaf(@PathVariable UUID id,
                                                                       @RequestParam UUID validateurId) {
        return ResponseEntity.ok(demandeActiviteService.approuverDemandeRaf(id, validateurId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_CHARGE_PROGRAMME') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeActiviteResponse> rejeterDemande(@PathVariable UUID id,
                                                                   @RequestParam String motif) {
        return ResponseEntity.ok(demandeActiviteService.rejeterDemande(id, motif));
    }
}
