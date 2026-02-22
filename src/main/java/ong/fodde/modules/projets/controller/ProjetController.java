package ong.fodde.modules.projets.controller;

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
import ong.fodde.modules.projets.dto.ProjetRequest;
import ong.fodde.modules.projets.dto.ProjetResponse;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.service.ProjetService;

@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RAF', 'ADMIN')")
    public ResponseEntity<ProjetResponse> creerProjet(@Valid @RequestBody ProjetRequest request) {
        ProjetResponse response = projetService.creerProjet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjetResponse>> listerProjets() {
        return ResponseEntity.ok(projetService.listerProjets());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProjetResponse> obtenirProjet(@PathVariable UUID id) {
        return ResponseEntity.ok(projetService.obtenirProjet(id));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('RAF', 'ADMIN')")
    public ResponseEntity<ProjetResponse> changerStatut(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        StatutProjet nouveauStatut = StatutProjet.valueOf(body.get("statut"));
        return ResponseEntity.ok(projetService.changerStatut(id, nouveauStatut));
    }

    @PostMapping("/{id}/equipe")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> ajouterMembre(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        projetService.ajouterMembreEquipe(id, body.get("userId"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjetResponse>> listerMesProjets(
            @RequestBody Map<String, UUID> body) {
        return ResponseEntity.ok(projetService.listerMesProjets(body.get("userId")));
    }
}
