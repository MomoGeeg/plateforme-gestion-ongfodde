package ong.fodde.modules.projets.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.dto.MontageDtos.BudgetRequest;
import ong.fodde.modules.projets.dto.MontageDtos.LigneBudgetResponse;
import ong.fodde.modules.projets.dto.MontageDtos.MontageResponse;
import ong.fodde.modules.projets.dto.MontageDtos.ObjectifRequest;
import ong.fodde.modules.projets.dto.MontageDtos.ObjectifResponse;
import ong.fodde.modules.projets.dto.MontageDtos.ResultatRequest;
import ong.fodde.modules.projets.dto.MontageDtos.ResultatResponse;
import ong.fodde.modules.projets.service.MontageProjetService;

/**
 * Montage d'un projet : cadre logique et budget prévisionnel. Les droits (RP, chef de projet ou
 * admin, projet en brouillon) sont vérifiés par le service.
 */
@RestController
@RequestMapping("/api/projets/{projetId}")
public class MontageProjetController {

    private final MontageProjetService service;

    public MontageProjetController(MontageProjetService service) {
        this.service = service;
    }

    /** Tout le projet en un appel : cadre logique, activités, indicateurs, budget, équipe, contrôles. */
    @GetMapping("/montage")
    @PreAuthorize("isAuthenticated()")
    public MontageResponse montage(@PathVariable UUID projetId, @AuthenticationPrincipal User user) {
        return service.montage(projetId, user);
    }

    @PostMapping("/objectifs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ObjectifResponse> ajouterObjectif(@PathVariable UUID projetId,
                                                            @Valid @RequestBody ObjectifRequest req,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouterObjectif(projetId, req, user));
    }

    @PutMapping("/objectifs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ObjectifResponse modifierObjectif(@PathVariable UUID projetId, @PathVariable UUID id,
                                             @Valid @RequestBody ObjectifRequest req,
                                             @AuthenticationPrincipal User user) {
        return service.modifierObjectif(projetId, id, req, user);
    }

    @DeleteMapping("/objectifs/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> supprimerObjectif(@PathVariable UUID projetId, @PathVariable UUID id,
                                                  @AuthenticationPrincipal User user) {
        service.supprimerObjectif(projetId, id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resultats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResultatResponse> ajouterResultat(@PathVariable UUID projetId,
                                                            @Valid @RequestBody ResultatRequest req,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouterResultat(projetId, req, user));
    }

    @PutMapping("/resultats/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResultatResponse modifierResultat(@PathVariable UUID projetId, @PathVariable UUID id,
                                             @Valid @RequestBody ResultatRequest req,
                                             @AuthenticationPrincipal User user) {
        return service.modifierResultat(projetId, id, req, user);
    }

    @DeleteMapping("/resultats/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> supprimerResultat(@PathVariable UUID projetId, @PathVariable UUID id,
                                                  @AuthenticationPrincipal User user) {
        service.supprimerResultat(projetId, id, user);
        return ResponseEntity.noContent().build();
    }

    /** Remplace tout le budget prévisionnel. */
    @PutMapping("/budget")
    @PreAuthorize("isAuthenticated()")
    public List<LigneBudgetResponse> enregistrerBudget(@PathVariable UUID projetId, @RequestBody BudgetRequest req,
                                                       @AuthenticationPrincipal User user) {
        return service.enregistrerBudget(projetId, req, user);
    }
}
