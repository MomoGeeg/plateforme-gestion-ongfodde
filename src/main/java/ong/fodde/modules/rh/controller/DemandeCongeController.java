package ong.fodde.modules.rh.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.rh.dto.DemandeCongeRequest;
import ong.fodde.modules.rh.dto.DemandeCongeResponse;
import ong.fodde.modules.rh.service.DemandeCongeService;
import ong.fodde.shared.securite.AccesPersonnel;

@RestController
@RequestMapping("/api/conges")
public class DemandeCongeController {

    private final DemandeCongeService demandeCongeService;
    private final AccesPersonnel accesPersonnel;

    public DemandeCongeController(DemandeCongeService demandeCongeService, AccesPersonnel accesPersonnel) {
        this.demandeCongeService = demandeCongeService;
        this.accesPersonnel = accesPersonnel;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeCongeResponse> soumettreDemande(@Valid @RequestBody DemandeCongeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(demandeCongeService.soumettreDemande(request));
    }

    @PatchMapping("/{id}/approuver")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeCongeResponse> approuverRAF(@PathVariable UUID id,
                                                              @AuthenticationPrincipal User currentUser) {
        UUID validateurId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(demandeCongeService.approuverRAF(id, validateurId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeCongeResponse> rejeter(@PathVariable UUID id,
                                                         @AuthenticationPrincipal User currentUser,
                                                         @RequestParam(required = false) String motif) {
        UUID validateurId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(demandeCongeService.rejeter(id, validateurId, motif));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DemandeCongeResponse> annuler(@PathVariable UUID id) {
        return ResponseEntity.ok(demandeCongeService.annuler(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<List<DemandeCongeResponse>> listerTous() {
        return ResponseEntity.ok(demandeCongeService.listerTous());
    }

    @GetMapping("/mes-conges/{employeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DemandeCongeResponse>> mesConges(@PathVariable UUID employeId,
                                                                @AuthenticationPrincipal User currentUser) {
        accesPersonnel.verifierEmploye(employeId, currentUser,
                "ROLE_RH", "ROLE_ADMIN", "ROLE_RAF", "ROLE_SE", "ROLE_RESPONSABLE_PROGRAMMES");
        return ResponseEntity.ok(demandeCongeService.mesConges(employeId));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<List<DemandeCongeResponse>> congesEnAttente() {
        return ResponseEntity.ok(demandeCongeService.congesEnAttente());
    }
}
