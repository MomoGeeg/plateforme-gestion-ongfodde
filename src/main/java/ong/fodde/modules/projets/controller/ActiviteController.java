package ong.fodde.modules.projets.controller;

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
import ong.fodde.modules.projets.dto.ActiviteRequest;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.entity.StatutActivite;
import ong.fodde.modules.projets.service.ActiviteService;

@RestController
@RequestMapping("/api/activites")
public class ActiviteController {

    private final ActiviteService activiteService;

    public ActiviteController(ActiviteService activiteService) {
        this.activiteService = activiteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ActiviteResponse> creerActivite(@Valid @RequestBody ActiviteRequest request) {
        ActiviteResponse response = activiteService.creerActivite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ActiviteResponse>> listerActivitesProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(activiteService.listerActivitesProjet(projetId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActiviteResponse> obtenirActivite(@PathVariable UUID id) {
        return ResponseEntity.ok(activiteService.obtenirActivite(id));
    }

    @PatchMapping("/{id}/avancement")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ActiviteResponse> mettreAJourAvancement(@PathVariable UUID id,
                                                                    @RequestParam(required = false) Integer taux,
                                                                    @RequestParam(required = false) StatutActivite statut) {
        return ResponseEntity.ok(activiteService.mettreAJourAvancement(id, taux, statut));
    }

    @PatchMapping("/{id}/demarrer")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_CHARGE_PROGRAMME') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ActiviteResponse> demarrerActivite(@PathVariable UUID id,
                                                              @RequestParam(required = false) LocalDate dateReelDebut) {
        return ResponseEntity.ok(activiteService.demarrerActivite(id, dateReelDebut));
    }
}
