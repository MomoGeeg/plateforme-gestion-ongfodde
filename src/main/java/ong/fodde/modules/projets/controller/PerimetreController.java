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
import ong.fodde.modules.projets.dto.PerimetreDtos.PerimetreRequest;
import ong.fodde.modules.projets.dto.PerimetreDtos.PerimetreResponse;
import ong.fodde.modules.projets.service.PerimetreService;
import ong.fodde.modules.projets.service.PerimetreService.Intervenants;

@RestController
@RequestMapping("/api/perimetres")
@PreAuthorize("isAuthenticated()")
public class PerimetreController {

    private final PerimetreService service;

    public PerimetreController(PerimetreService service) {
        this.service = service;
    }

    @GetMapping("/projet/{projetId}")
    public List<PerimetreResponse> parProjet(@PathVariable UUID projetId) {
        return service.listerParProjet(projetId);
    }

    @GetMapping("/mes-perimetres")
    public List<PerimetreResponse> mesPerimetres(@AuthenticationPrincipal User user) {
        return service.mesPerimetres(user);
    }

    /** Carte : tous les périmètres (RP, SE, administrateur ; vérifié par le service). */
    @GetMapping("/carte")
    public List<PerimetreResponse> carte(@AuthenticationPrincipal User user) {
        return service.carte(user);
    }

    @GetMapping("/intervenants")
    public Intervenants intervenants() {
        return service.intervenants();
    }

    /** Chef de projet, RP ou admin (vérifié par le service). */
    @PostMapping
    public ResponseEntity<PerimetreResponse> creer(@Valid @RequestBody PerimetreRequest request,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(request, user));
    }

    @PutMapping("/{id}")
    public PerimetreResponse modifier(@PathVariable UUID id, @Valid @RequestBody PerimetreRequest request,
                                      @AuthenticationPrincipal User user) {
        return service.modifier(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        service.supprimer(id, user);
        return ResponseEntity.noContent().build();
    }
}
