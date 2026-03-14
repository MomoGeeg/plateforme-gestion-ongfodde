package ong.fodde.modules.financier.controller;

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

import ong.fodde.modules.financier.dto.AllocationProjetRequest;
import ong.fodde.modules.financier.dto.AllocationProjetResponse;
import ong.fodde.modules.financier.dto.LigneBudgetaireRequest;
import ong.fodde.modules.financier.dto.LigneBudgetaireResponse;
import ong.fodde.modules.financier.service.AllocationProjetService;

@RestController
@RequestMapping("/api/allocations")
public class AllocationProjetController {

    private final AllocationProjetService allocationProjetService;

    public AllocationProjetController(AllocationProjetService allocationProjetService) {
        this.allocationProjetService = allocationProjetService;
    }

    // ── Lignes budgétaires d'un projet (saisie de dépense pour COMPTABLE) ───

    @GetMapping("/lignes")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_COMPTABLE') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<List<LigneBudgetaireResponse>> getLignesParProjet(@RequestParam UUID projetId) {
        return ResponseEntity.ok(allocationProjetService.getLignesByProjetId(projetId));
    }

    // ── Liste des allocations d'un budget annuel ──────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AllocationProjetResponse>> lister(
            @RequestParam UUID budgetAnnuelId) {
        return ResponseEntity.ok(allocationProjetService.listerParBudgetAnnuel(budgetAnnuelId));
    }

    // ── Allocations en attente de validation SE ───────────────────────────────

    @GetMapping("/en-validation")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AllocationProjetResponse>> listerEnValidation() {
        return ResponseEntity.ok(allocationProjetService.listerEnValidation());
    }

    // ── Créer une allocation ──────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AllocationProjetResponse> creer(
            @RequestBody AllocationProjetRequest request,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(allocationProjetService.creerAllocation(request, userId));
    }

    // ── Ajouter une ligne budgétaire ──────────────────────────────────────────

    @PostMapping("/{id}/lignes")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<LigneBudgetaireResponse> ajouterLigne(
            @PathVariable UUID id,
            @RequestBody LigneBudgetaireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(allocationProjetService.ajouterLigne(request));
    }

    // ── Workflow RAF → SE ─────────────────────────────────────────────────────

    @PatchMapping("/{id}/soumettre")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AllocationProjetResponse> soumettre(@PathVariable UUID id) {
        return ResponseEntity.ok(allocationProjetService.soumettreValidation(id));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AllocationProjetResponse> valider(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID validateurId = body.get("validateurId") != null
                ? UUID.fromString(body.get("validateurId")) : null;
        return ResponseEntity.ok(allocationProjetService.validerAllocation(id, validateurId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AllocationProjetResponse> rejeter(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String motif = body.getOrDefault("motif", "Rejet sans motif");
        return ResponseEntity.ok(allocationProjetService.rejeterAllocation(id, motif));
    }
}
