package ong.fodde.modules.missions.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
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
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<MissionResponse>> listerToutesMissions() {
        return ResponseEntity.ok(missionService.listerToutesMissions());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MissionResponse> soumettreMission(
            @Valid @RequestBody MissionRequest request,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                missionService.soumettreDemandesMission(request, userId));
    }

    @PatchMapping("/{id}/approuver-n1")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MissionResponse> approuverN1(@PathVariable UUID id) {
        return ResponseEntity.ok(missionService.approuverN1(id));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MissionResponse> validerMission(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UUID userId = body.get("userId") != null ? UUID.fromString(body.get("userId")) : null;
        return ResponseEntity.ok(missionService.validerMission(id, userId));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MissionResponse> rejeterMission(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String motif = body.get("motif");
        UUID userId = body.get("userId") != null ? UUID.fromString(body.get("userId")) : null;
        return ResponseEntity.ok(missionService.rejeterMission(id, motif, userId));
    }
}
