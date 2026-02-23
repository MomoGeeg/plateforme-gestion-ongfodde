package ong.fodde.modules.rh.controller;

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
import ong.fodde.modules.rh.dto.DemandeCongeRequest;
import ong.fodde.modules.rh.dto.DemandeCongeResponse;
import ong.fodde.modules.rh.entity.StatutConge;
import ong.fodde.modules.rh.service.DemandeCongeService;

@RestController
@RequestMapping("/api/conges")
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;

    public DemandeCongeController(DemandeCongeService demandeCongeService) {
        this.demandeCongeService = demandeCongeService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandeCongeResponse> soumettreDemande(@Valid @RequestBody DemandeCongeRequest request) {
        DemandeCongeResponse response = demandeCongeService.soumettreDemande(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/traiter")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeCongeResponse> traiterDemande(@PathVariable UUID id,
                                                                @RequestParam StatutConge statut,
                                                                @RequestParam UUID approbateurId,
                                                                @RequestParam(required = false) String commentaire) {
        return ResponseEntity.ok(demandeCongeService.traiterDemande(id, statut, approbateurId, commentaire));
    }

    @GetMapping("/mes-conges/{employeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DemandeCongeResponse>> mesConges(@PathVariable UUID employeId) {
        return ResponseEntity.ok(demandeCongeService.mesConges(employeId));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DemandeCongeResponse>> congesEnAttente() {
        return ResponseEntity.ok(demandeCongeService.congesEnAttente());
    }
}
