package ong.fodde.modules.financier.controller;

import java.math.BigDecimal;
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
import ong.fodde.modules.financier.dto.DepenseRequest;
import ong.fodde.modules.financier.dto.DepenseResponse;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.financier.service.DepenseService;

@RestController
@RequestMapping("/api/depenses")
public class DepenseController {

    private final DepenseService depenseService;

    public DepenseController(DepenseService depenseService) {
        this.depenseService = depenseService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DepenseResponse>> listerToutesDepenses(
            @RequestParam(required = false) StatutDepense statut) {
        return ResponseEntity.ok(depenseService.listerToutesDepenses(statut));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_COMPTABLE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DepenseResponse> soumettreDepense(
            @Valid @RequestBody DepenseRequest request,
            @RequestParam UUID userId) {
        DepenseResponse response = depenseService.soumettreDepense(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepenseResponse>> listerDepenses(@PathVariable UUID projetId) {
        return ResponseEntity.ok(depenseService.listerDepenses(projetId));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_COMPTABLE')")

    public ResponseEntity<DepenseResponse> changerStatut(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        StatutDepense nouveauStatut = StatutDepense.valueOf(body.get("statut"));
        UUID userId = UUID.fromString(body.get("userId"));
        String motif = body.get("motif");
        return ResponseEntity.ok(depenseService.changerStatut(id, nouveauStatut, userId, motif));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepenseResponse> getDepense(@PathVariable UUID id) {
        return ResponseEntity.ok(depenseService.getDepenseById(id));
    }

    @GetMapping("/mes-depenses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepenseResponse>> mesDepenses(@RequestParam UUID userId) {
        return ResponseEntity.ok(depenseService.mesDepenses(userId));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_COMPTABLE')")
    public ResponseEntity<DepenseResponse> annulerDepense(
            @PathVariable UUID id,
            @RequestParam String motif,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(depenseService.annulerDepense(id, motif, userId));
    }

    @GetMapping("/stats/mois-courant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, BigDecimal>> statsMoisCourant() {
        BigDecimal total = depenseService.totalDepensesMoisCourant();
        return ResponseEntity.ok(Map.of("total", total));
    }
}
