package ong.fodde.modules.ged.controller;

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

    @PostMapping
@PreAuthorize("isAuthenticated()")
public ResponseEntity<DocumentResponse> creerDocument(
        @Valid @RequestBody DocumentRequest request,
        @AuthenticationPrincipal User currentUser) {
    DocumentResponse response = documentService.creerDocument(request, currentUser.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponse>> listerDocuments() {
        return ResponseEntity.ok(documentService.listerDocuments());
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentResponse>> listerParProjet(@PathVariable UUID projetId) {
        return ResponseEntity.ok(documentService.listerParProjet(projetId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentResponse> obtenirDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.obtenirDocument(id));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DocumentResponse> validerDocument(@PathVariable UUID id,
                                                             @RequestParam UUID validateurId) {
        return ResponseEntity.ok(documentService.validerDocument(id, validateurId));
    }

    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DocumentResponse> archiverDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.archiverDocument(id));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VersionDocumentResponse> ajouterVersion(@PathVariable UUID id,
                                                                    @Valid @RequestBody VersionDocumentRequest request) {
        VersionDocumentResponse response = documentService.ajouterVersion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VersionDocumentResponse>> listerVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(documentService.listerVersions(id));
    }

    @GetMapping("/ca")
    @PreAuthorize("hasAuthority('ROLE_CA') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DocumentResponse>> documentsCA() {
        return ResponseEntity.ok(documentService.documentsCA());
    }
}
