package ong.fodde.modules.projets.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.dto.SuiviDtos.DecisionRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.MembreEquipe;
import ong.fodde.modules.projets.dto.SuiviDtos.ProjetDuSuivi;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviResponse;
import ong.fodde.modules.projets.service.SuiviTerrainService;

/** Planning et rapport mensuels du terrain : remise, visa du superviseur, validation du coordonnateur, diffusion. */
@RestController
@RequestMapping("/api/suivis-terrain")
@PreAuthorize("isAuthenticated()")
public class SuiviTerrainController {

    private final SuiviTerrainService service;

    public SuiviTerrainController(SuiviTerrainService service) {
        this.service = service;
    }

    @GetMapping("/miens")
    public List<SuiviResponse> miens(@AuthenticationPrincipal User user) {
        return service.mesSuivis(user);
    }

    @GetMapping("/mes-projets")
    public List<ProjetDuSuivi> mesProjets(@AuthenticationPrincipal User user) {
        return service.mesProjets(user);
    }

    /** Remise : `donnees` (type, mois, projetId, contenu) et, pour un rapport, `fichier` (Word). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_TECHNICIEN_TERRAIN') or hasAuthority('ROLE_ANIMATEUR')")
    public ResponseEntity<SuiviResponse> soumettre(@Valid @RequestPart("donnees") SuiviRequest request,
                                                   @RequestPart(value = "fichier", required = false) MultipartFile fichier,
                                                   @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.soumettre(request, fichier, user));
    }

    /** Version corrigée du rapport, déposée par le superviseur avant son visa. */
    @PostMapping(value = "/{id}/correction", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuiviResponse deposerCorrection(@PathVariable UUID id, @RequestPart("fichier") MultipartFile fichier,
                                           @AuthenticationPrincipal User user) {
        return service.deposerCorrection(id, fichier, user);
    }

    /** Fichier Word du document ; `initial=true` : la version remise par l'auteur. */
    @GetMapping("/{id}/fichier")
    public ResponseEntity<byte[]> telecharger(@PathVariable UUID id,
                                              @RequestParam(defaultValue = "false") boolean initial,
                                              @AuthenticationPrincipal User user) {
        SuiviTerrainService.Fichier f = service.telecharger(id, initial, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.nom(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(f.contentType()))
                .body(f.contenu());
    }

    @GetMapping("/equipe")
    public List<SuiviResponse> suivisEquipe(@AuthenticationPrincipal User user) {
        return service.suivisEquipe(user);
    }

    @GetMapping("/equipe/membres")
    public List<MembreEquipe> equipe(@AuthenticationPrincipal User user) {
        return service.equipe(user);
    }

    /** Plannings et rapports validés par le coordonnateur du projet. */
    @GetMapping("/valides")
    @PreAuthorize("hasAnyAuthority('ROLE_RAF', 'ROLE_RESPONSABLE_PROGRAMMES', 'ROLE_SE', 'ROLE_RESPONSABLE_SE', 'ROLE_ADMIN')")
    public List<SuiviResponse> valides(@AuthenticationPrincipal User user) {
        return service.valides(user);
    }

    /** Superviseur du technicien, puis coordonnateur du projet (vérifié par le service). */
    @PatchMapping("/{id}/decision")
    public SuiviResponse decider(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request,
                                 @AuthenticationPrincipal User user) {
        return service.decider(id, request, user);
    }
}
