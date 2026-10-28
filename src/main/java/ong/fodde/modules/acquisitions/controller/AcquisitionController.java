package ong.fodde.modules.acquisitions.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.acquisitions.dto.AcquisitionRequest;
import ong.fodde.modules.acquisitions.dto.AcquisitionResponse;
import ong.fodde.modules.acquisitions.service.AcquisitionService;

@RestController
@RequestMapping("/api/acquisitions")
public class AcquisitionController {

    private final AcquisitionService acquisitionService;

    public AcquisitionController(AcquisitionService acquisitionService) {
        this.acquisitionService = acquisitionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AcquisitionResponse>> listerToutesAcquisitions() {
        return ResponseEntity.ok(acquisitionService.listerToutesAcquisitions());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AcquisitionResponse> creerAcquisition(
            @Valid @RequestBody AcquisitionRequest request,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                acquisitionService.creerAcquisition(request, userId));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AcquisitionResponse> validerAcquisition(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID userId = body.get("userId") != null ? UUID.fromString(body.get("userId")) : null;
        return ResponseEntity.ok(acquisitionService.validerAcquisition(id, userId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AcquisitionResponse> rejeterAcquisition(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String motif = body.get("motif");
        UUID userId = body.get("userId") != null ? UUID.fromString(body.get("userId")) : null;
        return ResponseEntity.ok(acquisitionService.rejeterAcquisition(id, motif, userId));
    }
}
