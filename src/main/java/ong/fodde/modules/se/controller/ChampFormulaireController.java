package ong.fodde.modules.se.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.se.dto.ChampFormulaireRequest;
import ong.fodde.modules.se.dto.ChampFormulaireResponse;
import ong.fodde.modules.se.service.ChampFormulaireService;

@RestController
@RequestMapping("/api/formulaires/{formulaireId}/champs")
public class ChampFormulaireController {

    private final ChampFormulaireService champFormulaireService;

    public ChampFormulaireController(ChampFormulaireService champFormulaireService) {
        this.champFormulaireService = champFormulaireService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_RESPONSABLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ChampFormulaireResponse> ajouterChamp(@PathVariable UUID formulaireId,
                                                                  @Valid @RequestBody ChampFormulaireRequest request) {
        ChampFormulaireResponse response = champFormulaireService.ajouterChamp(formulaireId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChampFormulaireResponse>> listerChamps(@PathVariable UUID formulaireId) {
        return ResponseEntity.ok(champFormulaireService.listerChamps(formulaireId));
    }
}
