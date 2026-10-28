package ong.fodde.modules.financier.controller;

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

import ong.fodde.modules.financier.dto.BudgetAnnuelRequest;
import ong.fodde.modules.financier.dto.BudgetAnnuelResponse;
import ong.fodde.modules.financier.service.BudgetAnnuelService;

@RestController
@RequestMapping("/api/budgets-annuels")
public class BudgetAnnuelController {

    private final BudgetAnnuelService budgetAnnuelService;

    public BudgetAnnuelController(BudgetAnnuelService budgetAnnuelService) {
        this.budgetAnnuelService = budgetAnnuelService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<BudgetAnnuelResponse>> listerTous() {
        return ResponseEntity.ok(budgetAnnuelService.listerTous());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BudgetAnnuelResponse> obtenirParId(@PathVariable UUID id) {
        return ResponseEntity.ok(budgetAnnuelService.obtenirParId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BudgetAnnuelResponse> creer(
            @RequestBody BudgetAnnuelRequest request,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetAnnuelService.creerBudgetAnnuel(request, userId));
    }

    @PatchMapping("/{id}/activer")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BudgetAnnuelResponse> activer(@PathVariable UUID id) {
        return ResponseEntity.ok(budgetAnnuelService.activerBudget(id));
    }

    @PatchMapping("/{id}/cloturer")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<BudgetAnnuelResponse> cloturer(@PathVariable UUID id) {
        return ResponseEntity.ok(budgetAnnuelService.cloturerBudget(id));
    }
}
