package ong.fodde.modules.projets.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.dto.SuiviDtos.DecisionRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.MembreEquipe;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviResponse;
import ong.fodde.modules.projets.service.SuiviTerrainService;

/** Planning et rapport mensuels des techniciens, équipe du superviseur. */
@RestController
@RequestMapping("/api/suivis-terrain")
@PreAuthorize("isAuthenticated()")
public class SuiviTerrainController {

    private final SuiviTerrainService service;

    public SuiviTerrainController(SuiviTerrainService service) {
        this.service = service;
    }

    @GetMapping("/miens")
    public List<SuiviResponse> miens(@AuthenticationPrincipal User user) {
        return service.mesSuivis(user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TECHNICIEN_TERRAIN') or hasAuthority('ROLE_ANIMATEUR')")
    public ResponseEntity<SuiviResponse> soumettre(@Valid @RequestBody SuiviRequest request,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.soumettre(request, user));
    }

    @GetMapping("/equipe")
    public List<SuiviResponse> suivisEquipe(@AuthenticationPrincipal User user) {
        return service.suivisEquipe(user);
    }

    @GetMapping("/equipe/membres")
    public List<MembreEquipe> equipe(@AuthenticationPrincipal User user) {
        return service.equipe(user);
    }

    /** Superviseur du technicien ou coordonnateur de l'animateur (vérifié par le service). */
    @PatchMapping("/{id}/decision")
    public SuiviResponse decider(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request,
                                 @AuthenticationPrincipal User user) {
        return service.decider(id, request, user);
    }
}
