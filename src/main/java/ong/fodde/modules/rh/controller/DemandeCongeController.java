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

    @PatchMapping("/{id}/valider-n1")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<DemandeCongeResponse> validerN1(@PathVariable UUID id,
                                                           @RequestParam UUID validateurId) {
        return ResponseEntity.ok(demandeCongeService.validerN1(id, validateurId));
    }

    @PatchMapping("/{id}/approuver-rh")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeCongeResponse> approuverRH(@PathVariable UUID id,
                                                             @RequestParam UUID validateurId) {
        return ResponseEntity.ok(demandeCongeService.approuverRH(id, validateurId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<DemandeCongeResponse> rejeter(@PathVariable UUID id,
                                                         @RequestParam UUID validateurId,
                                                         @RequestParam(required = false) String motif) {
        return ResponseEntity.ok(demandeCongeService.rejeter(id, validateurId, motif));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandeCongeResponse> annuler(@PathVariable UUID id) {
        return ResponseEntity.ok(demandeCongeService.annuler(id));
    }

    @GetMapping("/mes-conges/{employeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DemandeCongeResponse>> mesConges(@PathVariable UUID employeId) {
        return ResponseEntity.ok(demandeCongeService.mesConges(employeId));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<List<DemandeCongeResponse>> congesEnAttente() {
        return ResponseEntity.ok(demandeCongeService.congesEnAttente());
    }

    @GetMapping("/valide-n1")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DemandeCongeResponse>> congesValideN1() {
        return ResponseEntity.ok(demandeCongeService.congesValideN1());
    }
}
