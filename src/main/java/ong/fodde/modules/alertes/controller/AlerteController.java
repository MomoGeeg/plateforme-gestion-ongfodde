package ong.fodde.modules.alertes.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ong.fodde.modules.alertes.dto.AlerteResponse;
import ong.fodde.modules.alertes.service.AlerteService;

@RestController
@RequestMapping("/api/alertes")
public class AlerteController {

    private final AlerteService alerteService;

    public AlerteController(AlerteService alerteService) {
        this.alerteService = alerteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AlerteResponse>> listerAlertes() {
        return ResponseEntity.ok(alerteService.listerAlertes());
    }

    @PatchMapping("/{id}/lire")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AlerteResponse> marquerLue(@PathVariable UUID id) {
        return ResponseEntity.ok(alerteService.marquerLue(id));
    }

    @PatchMapping("/tout-lire")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> marquerToutesLues() {
        alerteService.marquerToutesLues();
        return ResponseEntity.noContent().build();
    }
}
