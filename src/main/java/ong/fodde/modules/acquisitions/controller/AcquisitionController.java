package ong.fodde.modules.acquisitions.controller;

import java.util.List;
import java.util.Map;
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
import ong.fodde.modules.acquisitions.dto.AcquisitionRequest;
import ong.fodde.modules.acquisitions.dto.AcquisitionResponse;
import ong.fodde.modules.acquisitions.service.AcquisitionService;
import ong.fodde.modules.historique.dto.EtapeHistorique;

@RestController
@RequestMapping("/api/acquisitions")
public class AcquisitionController {

    private final AcquisitionService acquisitionService;

    public AcquisitionController(AcquisitionService acquisitionService) {
        this.acquisitionService = acquisitionService;
    }

    /** Toutes les acquisitions ; la logistique en a besoin pour inventorier celles qui sont validées. */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RESPONSABLE_LOGISTIQUE')")
    public ResponseEntity<List<AcquisitionResponse>> listerToutesAcquisitions() {
        return ResponseEntity.ok(acquisitionService.listerToutesAcquisitions());
    }

    /** Acquisitions demandées par l'utilisateur connecté. */
    @GetMapping("/mes-acquisitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AcquisitionResponse>> mesAcquisitions(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(acquisitionService.mesAcquisitions(currentUser.getId()));
    }

    /** Historique des validations : qui a fait quoi, quand, avec quel motif. */
    @GetMapping("/{id}/historique")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EtapeHistorique>> historique(@PathVariable UUID id,
                                                            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(acquisitionService.historique(id, currentUser));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcquisitionResponse> creerAcquisition(
            @Valid @RequestBody AcquisitionRequest request,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.status(HttpStatus.CREATED).body(
                acquisitionService.creerAcquisition(request, userId));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AcquisitionResponse> validerAcquisition(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(acquisitionService.validerAcquisition(id, userId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AcquisitionResponse> rejeterAcquisition(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String motif = body.get("motif");
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(acquisitionService.rejeterAcquisition(id, motif, userId));
    }
}
