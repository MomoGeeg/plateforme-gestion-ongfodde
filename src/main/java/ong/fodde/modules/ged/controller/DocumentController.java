package ong.fodde.modules.ged.controller;

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
import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.ged.dto.DocumentRequest;
import ong.fodde.modules.ged.dto.DocumentResponse;
import ong.fodde.modules.ged.dto.VersionDocumentRequest;
import ong.fodde.modules.ged.dto.VersionDocumentResponse;
import ong.fodde.modules.ged.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // ── Créer un document ──────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> creerDocument(
            @Valid @RequestBody DocumentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.creerDocument(request, currentUser.getId()));
    }

    // ── Lister documents accessibles ───────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponse>> listerDocuments(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) UUID employeId) {
        List<String> roles = currentUser.getRoles().stream().map(Role::getNom).toList();
        return ResponseEntity.ok(
                documentService.listerDocumentsAccessibles(currentUser.getId(), roles, employeId));
    }

    // ── File de validation (ROLE_SE uniquement) ────────────────────────────────

    @GetMapping("/en-validation")
    @PreAuthorize("hasAuthority('ROLE_SE')")
    public ResponseEntity<List<DocumentResponse>> listerEnValidation() {
        return ResponseEntity.ok(documentService.listerEnValidation());
    }

    // ── Détail d'un document ──────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> obtenirDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.obtenirDocument(id));
    }

    // ── Workflow validation ────────────────────────────────────────────────────

    @PatchMapping("/{id}/soumettre")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> soumettreValidation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(documentService.soumettreValidation(id, currentUser.getId()));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_SE')")
    public ResponseEntity<DocumentResponse> validerDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(documentService.validerDocument(id, currentUser.getId()));
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAuthority('ROLE_SE')")
    public ResponseEntity<DocumentResponse> rejeterDocument(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(documentService.rejeterDocument(id, body.get("motif")));
    }

    // ── Archiver ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DocumentResponse> archiverDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.archiverDocument(id));
    }

    // ── Versions ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VersionDocumentResponse> ajouterVersion(
            @PathVariable UUID id,
            @Valid @RequestBody VersionDocumentRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.ajouterVersion(id, request, currentUser.getId()));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VersionDocumentResponse>> listerVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.listerVersions(id));
    }
}
