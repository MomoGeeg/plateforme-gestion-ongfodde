package ong.fodde.modules.financier.controller;

import java.math.BigDecimal;
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
import ong.fodde.modules.financier.dto.DepenseRequest;
import ong.fodde.modules.financier.dto.DepenseResponse;
import ong.fodde.modules.historique.dto.EtapeHistorique;
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
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
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
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        StatutDepense nouveauStatut = StatutDepense.valueOf(body.get("statut"));
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        String motif = body.get("motif");
        return ResponseEntity.ok(depenseService.changerStatut(id, nouveauStatut, userId, motif));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DepenseResponse> getDepense(@PathVariable UUID id) {
        return ResponseEntity.ok(depenseService.getDepenseById(id));
    }

    /** Historique des validations : qui a fait quoi, quand, avec quel motif. */
    @GetMapping("/{id}/historique")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EtapeHistorique>> historique(@PathVariable UUID id,
                                                            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(depenseService.historique(id, currentUser));
    }

    @GetMapping("/mes-depenses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DepenseResponse>> mesDepenses(@AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(depenseService.mesDepenses(userId));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_COMPTABLE')")
    public ResponseEntity<DepenseResponse> annulerDepense(
            @PathVariable UUID id,
            @RequestParam String motif,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(depenseService.annulerDepense(id, motif, userId));
    }

    @GetMapping("/stats/mois-courant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, BigDecimal>> statsMoisCourant() {
        BigDecimal total = depenseService.totalDepensesMoisCourant();
        return ResponseEntity.ok(Map.of("total", total));
    }
}
