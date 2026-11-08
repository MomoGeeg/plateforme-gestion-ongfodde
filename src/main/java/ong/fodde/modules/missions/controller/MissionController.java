package ong.fodde.modules.missions.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.missions.dto.MissionDtos.Disponibilites;
import ong.fodde.modules.missions.dto.MissionDtos.MissionRequest;
import ong.fodde.modules.missions.dto.MissionDtos.MissionResponse;
import ong.fodde.modules.missions.dto.MissionDtos.OrganisationRequest;
import ong.fodde.modules.missions.dto.MissionDtos.PreparationRequest;
import ong.fodde.modules.missions.service.MissionService;

/**
 * Missions. L'auteur de chaque action vient toujours du jeton ; le service vérifie en plus
 * que c'est bien à lui d'agir (supérieur direct du demandeur, demandeur pour l'annulation…).
 */
@RestController
@RequestMapping("/api/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    /** Toutes les missions pour les rôles de gestion ; sinon celles qui concernent l'utilisateur. */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MissionResponse> lister(@AuthenticationPrincipal User user) {
        return missionService.lister(user);
    }

    @GetMapping("/mes-missions")
    @PreAuthorize("isAuthenticated()")
    public List<MissionResponse> mesMissions(@AuthenticationPrincipal User user) {
        return missionService.mesMissions(user);
    }

    /** Agents, chauffeurs et véhicules libres sur la période (missionId : la mission en cours de saisie). */
    @GetMapping("/disponibilites")
    @PreAuthorize("isAuthenticated()")
    public Disponibilites disponibilites(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(required = false) UUID missionId) {
        return missionService.disponibilites(debut, fin, missionId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public MissionResponse obtenir(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return missionService.obtenir(id, user);
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("isAuthenticated()")
    public List<EtapeHistorique> historique(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return missionService.historique(id, user);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MissionResponse> creer(@Valid @RequestBody MissionRequest request,
                                                 @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(missionService.creer(request, user));
    }

    /** Accord du supérieur direct (vérifié par le service). */
    @PatchMapping("/{id}/approuver-n1")
    @PreAuthorize("isAuthenticated()")
    public MissionResponse approuverN1(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return missionService.approuverN1(id, user);
    }

    @PatchMapping("/{id}/organiser")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_LOGISTIQUE') or hasAuthority('ROLE_ADMIN')")
    public MissionResponse organiser(@PathVariable UUID id, @RequestBody OrganisationRequest request,
                                     @AuthenticationPrincipal User user) {
        return missionService.organiser(id, request, user);
    }

    @PutMapping("/{id}/preparation")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public MissionResponse preparer(@PathVariable UUID id, @RequestBody PreparationRequest request,
                                    @AuthenticationPrincipal User user) {
        return missionService.preparer(id, request, user);
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public MissionResponse validerRaf(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return missionService.validerRaf(id, user);
    }

    @PatchMapping("/{id}/valider-se")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public MissionResponse validerSe(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return missionService.validerSe(id, user);
    }

    @PatchMapping("/{id}/debloquer-fonds")
    @PreAuthorize("hasAuthority('ROLE_COMPTABLE') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public MissionResponse debloquerFonds(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return missionService.debloquerFonds(id, user);
    }

    /** Rejet : le service vérifie que l'utilisateur a la main à cette étape. */
    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("isAuthenticated()")
    public MissionResponse rejeter(@PathVariable UUID id, @RequestBody Map<String, String> body,
                                   @AuthenticationPrincipal User user) {
        return missionService.rejeter(id, body.get("motif"), user);
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("isAuthenticated()")
    public MissionResponse annuler(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body,
                                   @AuthenticationPrincipal User user) {
        return missionService.annuler(id, body == null ? null : body.get("motif"), user);
    }
}
