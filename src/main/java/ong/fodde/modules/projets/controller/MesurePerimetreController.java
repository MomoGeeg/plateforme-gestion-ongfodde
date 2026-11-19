package ong.fodde.modules.projets.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.dto.MesureDtos.IndicateursDuPerimetre;
import ong.fodde.modules.projets.dto.MesureDtos.MesureRequest;
import ong.fodde.modules.projets.dto.MesureDtos.MesureResponse;
import ong.fodde.modules.projets.service.MesurePerimetreService;

/** Indicateurs relevés par périmètre. */
@RestController
@RequestMapping("/api/perimetres")
@PreAuthorize("isAuthenticated()")
public class MesurePerimetreController {

    private final MesurePerimetreService service;

    public MesurePerimetreController(MesurePerimetreService service) {
        this.service = service;
    }

    @GetMapping("/mes-indicateurs")
    public List<IndicateursDuPerimetre> mesIndicateurs(@AuthenticationPrincipal User user) {
        return service.mesIndicateurs(user);
    }

    @GetMapping("/{id}/indicateurs")
    public IndicateursDuPerimetre indicateurs(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return service.indicateurs(id, user);
    }

    @GetMapping("/{id}/mesures")
    public List<MesureResponse> mesures(@PathVariable UUID id) {
        return service.mesures(id);
    }

    /** Technicien ou superviseur du périmètre, chef de projet, RP, suivi-évaluation (vérifié par le service). */
    @PostMapping("/{id}/mesures")
    public ResponseEntity<MesureResponse> enregistrer(@PathVariable UUID id, @Valid @RequestBody MesureRequest request,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.enregistrer(id, request, user));
    }
}
