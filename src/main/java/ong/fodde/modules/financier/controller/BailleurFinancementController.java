package ong.fodde.modules.financier.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.financier.dto.BailleurRequest;
import ong.fodde.modules.financier.dto.BailleurResponse;
import ong.fodde.modules.financier.dto.FinancementRequest;
import ong.fodde.modules.financier.dto.FinancementResponse;
import ong.fodde.modules.financier.service.BailleurFinancementService;

/** Bailleurs de fonds et financements (conventions) des projets. */
@RestController
public class BailleurFinancementController {

    private static final String LECTURE =
        "hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')"
        + " or hasAuthority('ROLE_COMPTABLE') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')";
    private static final String GESTION =
        "hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')";

    private final BailleurFinancementService service;

    public BailleurFinancementController(BailleurFinancementService service) {
        this.service = service;
    }

    @GetMapping("/api/bailleurs")
    @PreAuthorize(LECTURE)
    public ResponseEntity<List<BailleurResponse>> listerBailleurs() {
        return ResponseEntity.ok(service.listerBailleurs());
    }

    @PostMapping("/api/bailleurs")
    @PreAuthorize(GESTION)
    public ResponseEntity<BailleurResponse> creerBailleur(@Valid @RequestBody BailleurRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerBailleur(request));
    }

    @GetMapping("/api/financements")
    @PreAuthorize(LECTURE)
    public ResponseEntity<List<FinancementResponse>> listerFinancements(
            @RequestParam(required = false) UUID projetId,
            @RequestParam(required = false) UUID bailleurId) {
        return ResponseEntity.ok(service.listerFinancements(projetId, bailleurId));
    }

    @PostMapping("/api/financements")
    @PreAuthorize(GESTION)
    public ResponseEntity<FinancementResponse> creerFinancement(
            @Valid @RequestBody FinancementRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creerFinancement(request, currentUser.getId()));
    }
}
