package ong.fodde.modules.ged.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.ged.dto.DocumentRequest;
import ong.fodde.modules.ged.dto.DocumentResponse;
import ong.fodde.modules.ged.dto.VersionDocumentRequest;
import ong.fodde.modules.ged.dto.VersionDocumentResponse;
import ong.fodde.modules.ged.entity.Document;
import ong.fodde.modules.ged.entity.StatutDocument;
import ong.fodde.modules.ged.entity.VersionDocument;
import ong.fodde.modules.ged.entity.VisibiliteDocument;
import ong.fodde.modules.ged.repository.DocumentRepository;
import ong.fodde.modules.ged.repository.VersionDocumentRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final VersionDocumentRepository versionDocumentRepository;

    public DocumentService(DocumentRepository documentRepository,
                           VersionDocumentRepository versionDocumentRepository) {
        this.documentRepository = documentRepository;
        this.versionDocumentRepository = versionDocumentRepository;
    }

    public DocumentResponse creerDocument(DocumentRequest request, UUID createdBy) {
        Document document = new Document();
        document.setTitre(request.titre());
        document.setDescription(request.description());
        document.setTypeDocument(request.typeDocument());
        document.setVisibilite(request.visibilite() != null ? request.visibilite() : VisibiliteDocument.PRIVE);
        document.setProjetId(request.projetId());
        document.setEmployeId(request.employeId());
        document.setAccessibleCa(request.accessibleCa() != null ? request.accessibleCa() : false);
        document.setStatut(StatutDocument.BROUILLON);
        document.setVersionActuelle(1);
        document.setCreatedBy(createdBy);

        Document saved = documentRepository.save(document);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerDocuments() {
        return documentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerParProjet(UUID projetId) {
        return documentRepository.findByProjetId(projetId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse obtenirDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'id : " + id));
        return toResponse(document);
    }

    public DocumentResponse validerDocument(UUID id, UUID validateurId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'id : " + id));

        if (document.getStatut() != StatutDocument.EN_VALIDATION) {
            throw new BusinessException("Seuls les documents en validation peuvent être validés");
        }

        document.setStatut(StatutDocument.VALIDE);
        document.setDateValidation(LocalDateTime.now());
        document.setValideParId(validateurId);

        Document saved = documentRepository.save(document);
        return toResponse(saved);
    }

    public DocumentResponse archiverDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé avec l'id : " + id));

        document.setStatut(StatutDocument.ARCHIVE);

        Document saved = documentRepository.save(document);
        return toResponse(saved);
    }

    public VersionDocumentResponse ajouterVersion(VersionDocumentRequest request) {
        Document document = documentRepository.findById(request.documentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document non trouvé avec l'id : " + request.documentId()));

        int nouvelleVersion = document.getVersionActuelle() + 1;
        document.setVersionActuelle(nouvelleVersion);
        documentRepository.save(document);

        VersionDocument version = new VersionDocument();
        version.setDocumentId(request.documentId());
        version.setVersion(nouvelleVersion);
        version.setUrl(request.url());
        version.setTaille(request.taille());
        version.setTypeFichier(request.typeFichier());
        version.setUploadePar(request.uploadePar());
        version.setUploadeLe(LocalDateTime.now());

        VersionDocument saved = versionDocumentRepository.save(version);
        return toVersionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<VersionDocumentResponse> listerVersions(UUID documentId) {
        return versionDocumentRepository.findByDocumentIdOrderByVersionDesc(documentId).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> documentsCA() {
        return documentRepository.findByAccessibleCaTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getTitre(),
                d.getDescription(),
                d.getTypeDocument(),
                d.getStatut(),
                d.getVisibilite(),
                d.getProjetId(),
                d.getEmployeId(),
                d.getVersionActuelle(),
                d.getAccessibleCa(),
                d.getDateValidation(),
                d.getValideParId(),
                d.getCreatedAt()
        );
    }

    private VersionDocumentResponse toVersionResponse(VersionDocument v) {
        return new VersionDocumentResponse(
                v.getId(),
                v.getDocumentId(),
                v.getVersion(),
                v.getUrl(),
                v.getTaille(),
                v.getTypeFichier(),
                v.getUploadePar(),
                v.getUploadeLe()
        );
    }
}
