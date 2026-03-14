package ong.fodde.modules.logistique.controller;

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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ong.fodde.modules.logistique.dto.BienRequest;
import ong.fodde.modules.logistique.dto.BienResponse;
import ong.fodde.modules.logistique.dto.MouvementRequest;
import ong.fodde.modules.logistique.dto.MouvementResponse;
import ong.fodde.modules.logistique.dto.VehiculeRequest;
import ong.fodde.modules.logistique.dto.VehiculeResponse;
import ong.fodde.modules.logistique.service.LogistiqueService;

@RestController
@RequestMapping("/api/logistique")
@RequiredArgsConstructor
public class LogistiqueController {

    private final LogistiqueService logistiqueService;

    // ── KPI ──────────────────────────────────────────────────
    @GetMapping("/kpi")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getKpi() {
        return ResponseEntity.ok(logistiqueService.getKpi());
    }

    // ── BIENS ────────────────────────────────────────────────
    @GetMapping("/biens")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<List<BienResponse>> listerBiens() {
        return ResponseEntity.ok(logistiqueService.listerBiens());
    }

    @GetMapping("/biens/{id}")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<BienResponse> getBien(@PathVariable UUID id) {
        return ResponseEntity.ok(logistiqueService.getBien(id));
    }

    @PostMapping("/biens")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_ADMIN')")
    public ResponseEntity<BienResponse> creerBien(@Valid @RequestBody BienRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logistiqueService.creerBien(req));
    }

    @PutMapping("/biens/{id}")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_ADMIN')")
    public ResponseEntity<BienResponse> modifierBien(@PathVariable UUID id, @Valid @RequestBody BienRequest req) {
        return ResponseEntity.ok(logistiqueService.modifierBien(id, req));
    }

    @PatchMapping("/biens/{id}/hors-service")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_ADMIN')")
    public ResponseEntity<Void> mettreHorsService(@PathVariable UUID id) {
        logistiqueService.mettreHorsService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/biens/{id}/mouvements")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<List<MouvementResponse>> getMouvementsBien(@PathVariable UUID id) {
        return ResponseEntity.ok(logistiqueService.getMouvementsBien(id));
    }

    // ── VEHICULES ────────────────────────────────────────────
    @GetMapping("/vehicules")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<List<VehiculeResponse>> listerVehicules() {
        return ResponseEntity.ok(logistiqueService.listerVehicules());
    }

    @GetMapping("/vehicules/{id}")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<VehiculeResponse> getVehicule(@PathVariable UUID id) {
        return ResponseEntity.ok(logistiqueService.getVehicule(id));
    }

    @PostMapping("/vehicules")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_ADMIN')")
    public ResponseEntity<VehiculeResponse> creerVehicule(@Valid @RequestBody VehiculeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logistiqueService.creerVehicule(req));
    }

    @PutMapping("/vehicules/{id}")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_ADMIN')")
    public ResponseEntity<VehiculeResponse> modifierVehicule(@PathVariable UUID id, @Valid @RequestBody VehiculeRequest req) {
        return ResponseEntity.ok(logistiqueService.modifierVehicule(id, req));
    }

    @GetMapping("/vehicules/alertes")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<List<VehiculeResponse>> getAlertes() {
        return ResponseEntity.ok(logistiqueService.getAlertes());
    }

    @GetMapping("/vehicules/{id}/mouvements")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<List<MouvementResponse>> getMouvementsVehicule(@PathVariable UUID id) {
        return ResponseEntity.ok(logistiqueService.getMouvementsVehicule(id));
    }

    // ── MOUVEMENTS ───────────────────────────────────────────
    @GetMapping("/mouvements")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_SE', 'ROLE_RAF', 'ROLE_ADMIN')")
    public ResponseEntity<List<MouvementResponse>> listerMouvements() {
        return ResponseEntity.ok(logistiqueService.listerMouvements());
    }

    @PostMapping("/mouvements")
    @PreAuthorize("hasAnyRole('ROLE_RESPONSABLE_LOGISTIQUE', 'ROLE_ADMIN')")
    public ResponseEntity<MouvementResponse> enregistrerMouvement(@Valid @RequestBody MouvementRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logistiqueService.enregistrerMouvement(req));
    }
}
