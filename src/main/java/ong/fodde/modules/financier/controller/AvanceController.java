package ong.fodde.modules.financier.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import ong.fodde.modules.financier.dto.AvanceRequest;
import ong.fodde.modules.financier.dto.AvanceResponse;
import ong.fodde.modules.financier.service.AvanceService;

@RestController
@RequestMapping("/api/avances")
public class AvanceController {

    private final AvanceService avanceService;

    public AvanceController(AvanceService avanceService) {
        this.avanceService = avanceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AvanceResponse> creerAvance(@Valid @RequestBody AvanceRequest request) {
        AvanceResponse response = avanceService.creerAvance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AvanceResponse>> listerAvancesProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(avanceService.listerAvancesProjet(projetId));
    }

    @GetMapping("/beneficiaire/{beneficiaireId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AvanceResponse>> listerAvancesBeneficiaire(@PathVariable UUID beneficiaireId) {
        return ResponseEntity.ok(avanceService.listerAvancesBeneficiaire(beneficiaireId));
    }

    @PatchMapping("/{id}/justifier")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AvanceResponse> justifierAvance(@PathVariable UUID id,
                                                           @RequestParam BigDecimal montantJustifie,
                                                           @RequestParam(required = false) LocalDate dateJustification) {
        return ResponseEntity.ok(avanceService.justifierAvance(id, montantJustifie, dateJustification));
    }
}
