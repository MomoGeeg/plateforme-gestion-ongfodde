package ong.fodde.modules.missions.controller;

import java.util.List;
import java.util.Map;
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
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.missions.dto.MissionRequest;
import ong.fodde.modules.missions.dto.MissionResponse;
import ong.fodde.modules.missions.service.MissionService;

@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_RESPONSABLE_PROGRAMMES')")
    public ResponseEntity<List<MissionResponse>> listerToutesMissions() {
        return ResponseEntity.ok(missionService.listerToutesMissions());
    }

    /** Missions demandées par l'utilisateur connecté (pour suivre leur statut). */
    @GetMapping("/mes-missions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MissionResponse>> mesMissions(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(missionService.mesMissions(currentUser.getId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MissionResponse> soumettreMission(
            @Valid @RequestBody MissionRequest request,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.status(HttpStatus.CREATED).body(
                missionService.soumettreDemandesMission(request, userId));
    }

    @PatchMapping("/{id}/approuver-n1")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MissionResponse> approuverN1(@PathVariable UUID id,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(missionService.approuverN1(id, currentUser.getId()));
    }

    /** Historique des validations : qui a fait quoi, quand, avec quel motif. */
    @GetMapping("/{id}/historique")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EtapeHistorique>> historique(@PathVariable UUID id,
                                                            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(missionService.historique(id, currentUser));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MissionResponse> validerMission(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(missionService.validerMission(id, userId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MissionResponse> rejeterMission(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User currentUser) {
        String motif = body.get("motif");
        UUID userId = currentUser.getId(); // jamais l'identifiant envoyé par le navigateur
        return ResponseEntity.ok(missionService.rejeterMission(id, motif, userId));
    }
}
