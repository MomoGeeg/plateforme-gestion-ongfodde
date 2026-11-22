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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.dto.AnimateurDtos.AffectationRequest;
import ong.fodde.modules.projets.dto.AnimateurDtos.AnimateurResponse;
import ong.fodde.modules.projets.dto.AnimateurDtos.PieceResponse;
import ong.fodde.modules.projets.dto.AnimateurDtos.ProjetAnimateur;
import ong.fodde.modules.projets.service.AnimateurService;
import ong.fodde.modules.projets.service.PieceActiviteService;

/** Animateurs des projets et pièces des activités (droits vérifiés par les services). */
@RestController
@PreAuthorize("isAuthenticated()")
public class AnimateurController {

    private final AnimateurService animateurService;
    private final PieceActiviteService pieceService;

    public AnimateurController(AnimateurService animateurService, PieceActiviteService pieceService) {
        this.animateurService = animateurService;
        this.pieceService = pieceService;
    }

    @GetMapping("/api/projets/{projetId}/animateurs")
    public List<AnimateurResponse> animateurs(@PathVariable UUID projetId) {
        return animateurService.animateursDuProjet(projetId);
    }

    @PostMapping("/api/projets/{projetId}/animateurs")
    public ResponseEntity<AnimateurResponse> affecter(@PathVariable UUID projetId,
                                                      @Valid @RequestBody AffectationRequest request,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(animateurService.affecter(projetId, request.animateurId(), user));
    }

    @DeleteMapping("/api/projets/{projetId}/animateurs/{animateurId}")
    public ResponseEntity<Void> retirer(@PathVariable UUID projetId, @PathVariable UUID animateurId,
                                        @AuthenticationPrincipal User user) {
        animateurService.retirer(projetId, animateurId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/animateurs/disponibles")
    public List<AnimateurResponse> disponibles() {
        return animateurService.disponibles();
    }

    @GetMapping("/api/animateurs/mes-projets")
    public List<ProjetAnimateur> mesProjets(@AuthenticationPrincipal User user) {
        return animateurService.mesProjets(user);
    }

    @GetMapping("/api/activites/{activiteId}/pieces")
    public List<PieceResponse> pieces(@PathVariable UUID activiteId, @AuthenticationPrincipal User user) {
        return pieceService.lister(activiteId, user);
    }

    @PostMapping(value = "/api/activites/{activiteId}/liste-presence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ActiviteResponse listePresence(@PathVariable UUID activiteId, @RequestParam("fichier") MultipartFile fichier,
                                          @AuthenticationPrincipal User user) {
        return pieceService.deposerListePresence(activiteId, fichier, user);
    }

    @PostMapping(value = "/api/activites/{activiteId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PieceResponse> photo(@PathVariable UUID activiteId, @RequestParam("fichier") MultipartFile fichier,
                                               @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pieceService.ajouterPhoto(activiteId, fichier, user));
    }

    @DeleteMapping("/api/activites/{activiteId}/photos/{pieceId}")
    public ResponseEntity<Void> retirerPhoto(@PathVariable UUID activiteId, @PathVariable UUID pieceId,
                                             @AuthenticationPrincipal User user) {
        pieceService.retirerPhoto(activiteId, pieceId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/activites/{activiteId}/pieces/{pieceId}/fichier")
    public ResponseEntity<byte[]> telecharger(@PathVariable UUID activiteId, @PathVariable UUID pieceId,
                                              @AuthenticationPrincipal User user) {
        PieceActiviteService.Fichier f = pieceService.telecharger(activiteId, pieceId, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(f.nom(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(f.contentType()))
                .body(f.contenu());
    }
}
