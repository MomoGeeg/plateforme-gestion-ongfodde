package ong.fodde.modules.financier.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.financier.dto.PieceJustificativeResponse;
import ong.fodde.modules.financier.service.PieceJustificativeService;

/** Pièces justificatives d'une dépense : joindre, lister, télécharger, retirer. */
@RestController
@RequestMapping("/api/depenses/{depenseId}/pieces")
public class PieceJustificativeController {

    private final PieceJustificativeService service;

    public PieceJustificativeController(PieceJustificativeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PieceJustificativeResponse>> lister(@PathVariable UUID depenseId,
                                                                   @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(service.lister(depenseId, currentUser));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PieceJustificativeResponse> ajouter(@PathVariable UUID depenseId,
                                                              @RequestParam("fichier") MultipartFile fichier,
                                                              @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ajouter(depenseId, fichier, currentUser));
    }

    @GetMapping("/{pieceId}/fichier")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> telecharger(@PathVariable UUID depenseId,
                                              @PathVariable UUID pieceId,
                                              @AuthenticationPrincipal User currentUser) {
        PieceJustificativeService.FichierPiece f = service.telecharger(depenseId, pieceId, currentUser);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(f.nomFichier(), StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .contentType(MediaType.parseMediaType(f.contentType()))
            .body(f.contenu());
    }

    @DeleteMapping("/{pieceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> supprimer(@PathVariable UUID depenseId,
                                          @PathVariable UUID pieceId,
                                          @AuthenticationPrincipal User currentUser) {
        service.supprimer(depenseId, pieceId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
