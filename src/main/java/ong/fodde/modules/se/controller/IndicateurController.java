package ong.fodde.modules.se.controller;

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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.se.dto.IndicateurRequest;
import ong.fodde.modules.se.dto.IndicateurResponse;
import ong.fodde.modules.se.service.IndicateurService;

@RestController
@RequestMapping("/api/indicateurs")
public class IndicateurController {

    private final IndicateurService indicateurService;

    public IndicateurController(IndicateurService indicateurService) {
        this.indicateurService = indicateurService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<IndicateurResponse> creerIndicateur(@Valid @RequestBody IndicateurRequest request) {
        IndicateurResponse response = indicateurService.creerIndicateur(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<IndicateurResponse>> listerIndicateurs(@PathVariable UUID projetId) {
        return ResponseEntity.ok(indicateurService.listerIndicateurs(projetId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IndicateurResponse> obtenirIndicateur(@PathVariable UUID id) {
        return ResponseEntity.ok(indicateurService.obtenirIndicateur(id));
    }

    @PatchMapping("/{id}/valeur")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<IndicateurResponse> mettreAJourValeur(
            @PathVariable UUID id,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal valeur = body.get("valeur");
        return ResponseEntity.ok(indicateurService.mettreAJourValeurAtteinte(id, valeur));
    }
}
