package ong.fodde.modules.financier.controller;

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
import ong.fodde.modules.financier.dto.BudgetRequest;
import ong.fodde.modules.financier.dto.BudgetResponse;
import ong.fodde.modules.financier.dto.LigneBudgetaireRequest;
import ong.fodde.modules.financier.dto.LigneBudgetaireResponse;
import ong.fodde.modules.financier.service.BudgetService;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BudgetResponse> creerBudget(@Valid @RequestBody BudgetRequest request) {
        BudgetResponse response = budgetService.creerBudget(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BudgetResponse> obtenirBudgetParProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(budgetService.obtenirBudgetParProjet(projetId));
    }

    @PostMapping("/{id}/lignes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LigneBudgetaireResponse> ajouterLigne(
            @PathVariable UUID id,
            @Valid @RequestBody LigneBudgetaireRequest request) {
        // On force le budgetId depuis l'URL pour cohérence
        LigneBudgetaireRequest requestAvecId = new LigneBudgetaireRequest(
            id, request.code(), request.intitule(), request.categorie(), request.montantPrevu()
        );
        LigneBudgetaireResponse response = budgetService.ajouterLigne(requestAvecId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/lignes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LigneBudgetaireResponse>> listerLignes(@PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.listerLignes(id));
    }
}
