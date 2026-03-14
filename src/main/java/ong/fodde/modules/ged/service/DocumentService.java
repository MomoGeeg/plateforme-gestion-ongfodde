package ong.fodde.modules.ged.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.ged.dto.DocumentRequest;
import ong.fodde.modules.ged.dto.DocumentResponse;
import ong.fodde.modules.ged.dto.VersionDocumentRequest;
import ong.fodde.modules.ged.dto.VersionDocumentResponse;
import ong.fodde.modules.ged.entity.Document;
import ong.fodde.modules.ged.entity.StatutDocument;
import ong.fodde.modules.ged.entity.TypeDocument;
import ong.fodde.modules.ged.entity.VersionDocument;
import ong.fodde.modules.ged.repository.DocumentRepository;
import ong.fodde.modules.ged.repository.VersionDocumentRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class DocumentService {

    // Types nécessitant une validation SE avant d'être considérés officiels
    private static final Set<TypeDocument> VALIDATION_REQUIRED = Set.of(
            TypeDocument.RAPPORT_ACTIVITE,
            TypeDocument.RAPPORT_SUIVI,
            TypeDocument.RAPPORT_EVALUATION,
            TypeDocument.RAPPORT_FINANCIER,
            TypeDocument.CONVENTION_BAILLEUR
    );

    // Rôles super-admins : accès total en lecture + écriture (toujours ajoutés)
    private static final Set<String> SUPER_ROLES = Set.of("ROLE_SE", "ROLE_ADMIN");

    // ACL par défaut : (rolesLecture, rolesEcriture) selon le type de document
    private static final Map<TypeDocument, Set<String>[]> DEFAULT_ACL = Map.ofEntries(
            Map.entry(TypeDocument.JUSTIFICATIF,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_COMPTABLE","ROLE_RESPONSABLE_PROGRAMMES"), Set.of("ROLE_SE","ROLE_RAF"))),
            Map.entry(TypeDocument.FACTURE,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_COMPTABLE","ROLE_RESPONSABLE_PROGRAMMES"), Set.of("ROLE_SE","ROLE_RAF","ROLE_COMPTABLE"))),
            Map.entry(TypeDocument.RAPPORT_FINANCIER,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_COMPTABLE","ROLE_RESPONSABLE_PROGRAMMES"), Set.of("ROLE_SE","ROLE_RAF"))),
            Map.entry(TypeDocument.CONTRAT_PRESTATAIRE,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES"), Set.of("ROLE_SE","ROLE_RAF"))),
            Map.entry(TypeDocument.ACQUISITION,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES"), Set.of("ROLE_SE","ROLE_RAF"))),
            Map.entry(TypeDocument.CONTRAT_EMPLOI,
                    roles(Set.of("ROLE_SE","ROLE_RH","ROLE_RAF"), Set.of("ROLE_SE","ROLE_RH"))),
            Map.entry(TypeDocument.BULLETIN_SALAIRE,
                    roles(Set.of("ROLE_SE","ROLE_RH","ROLE_RAF"), Set.of("ROLE_SE","ROLE_RH"))),
            Map.entry(TypeDocument.EVALUATION_RH,
                    roles(Set.of("ROLE_SE","ROLE_RH"), Set.of("ROLE_SE","ROLE_RH"))),
            Map.entry(TypeDocument.RAPPORT_ACTIVITE,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_PROGRAMMES","ROLE_CHARGE_PROGRAMME"))),
            Map.entry(TypeDocument.RAPPORT_MISSION,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_PROGRAMMES","ROLE_CHARGE_PROGRAMME","ROLE_TECHNICIEN_TERRAIN"))),
            Map.entry(TypeDocument.PLAN_TRAVAIL,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_PROGRAMMES","ROLE_CHARGE_PROGRAMME","ROLE_TECHNICIEN_TERRAIN"))),
            Map.entry(TypeDocument.RAPPORT_SUIVI,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"))),
            Map.entry(TypeDocument.RAPPORT_EVALUATION,
                    roles(Set.of("ROLE_SE","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_SE"))),
            Map.entry(TypeDocument.PLAN_SE,
                    roles(Set.of("ROLE_SE","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_SE"))),
            Map.entry(TypeDocument.DONNEES_COLLECTE,
                    roles(Set.of("ROLE_SE","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME","ROLE_TECHNICIEN_TERRAIN"))),
            Map.entry(TypeDocument.PHOTO_TERRAIN,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME","ROLE_TECHNICIEN_TERRAIN"),
                            Set.of("ROLE_SE","ROLE_RESPONSABLE_PROGRAMMES","ROLE_CHARGE_PROGRAMME","ROLE_TECHNICIEN_TERRAIN"))),
            Map.entry(TypeDocument.CONVENTION_BAILLEUR,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RESPONSABLE_PROGRAMMES"), Set.of("ROLE_SE"))),
            Map.entry(TypeDocument.INSTITUTIONNEL,
                    roles(Set.of("ROLE_SE","ROLE_RAF","ROLE_RH","ROLE_RESPONSABLE_PROGRAMMES","ROLE_RESPONSABLE_SE","ROLE_CHARGE_PROGRAMME"),
                            Set.of("ROLE_SE"))),
            Map.entry(TypeDocument.PV_REUNION,
                    roles(Set.of("ROLE_SE","ROLE_RH","ROLE_RESPONSABLE_PROGRAMMES"), Set.of("ROLE_SE")))
    );

    @SuppressWarnings("unchecked")
    private static Set<String>[] roles(Set<String> lecture, Set<String> ecriture) {
        return new Set[]{lecture, ecriture};
    }

    private final DocumentRepository documentRepository;
    private final VersionDocumentRepository versionDocumentRepository;
    private final UserRepository userRepository;

    public DocumentService(DocumentRepository documentRepository,
                           VersionDocumentRepository versionDocumentRepository,
                           UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.versionDocumentRepository = versionDocumentRepository;
        this.userRepository = userRepository;
    }

    // ── Créer un document ──────────────────────────────────────────────────────

    public DocumentResponse creerDocument(DocumentRequest request, UUID createdBy) {
        Document document = new Document();
        document.setTitre(request.titre());
        document.setDescription(request.description());
        document.setTypeDocument(request.typeDocument());
        document.setProjetId(request.projetId());
        document.setConcerneEmployeId(request.concerneEmployeId());
        document.setVersionActuelle(0);
        document.setCreatedBy(createdBy);

        boolean requiresValidation = VALIDATION_REQUIRED.contains(request.typeDocument());
        document.setRequiresValidation(requiresValidation);
        // Documents sans workflow de validation → directement VALIDE
        document.setStatut(requiresValidation ? StatutDocument.BROUILLON : StatutDocument.VALIDE);

        // ACL : utiliser les valeurs de la requête ou les valeurs par défaut du type
        Set<String>[] defaultAcl = DEFAULT_ACL.getOrDefault(request.typeDocument(),
                roles(Set.of("ROLE_SE"), Set.of("ROLE_SE")));

        Set<String> lecture = new HashSet<>(request.rolesLecture() != null ? request.rolesLecture() : defaultAcl[0]);
        Set<String> ecriture = new HashSet<>(request.rolesEcriture() != null ? request.rolesEcriture() : defaultAcl[1]);

        // ROLE_SE et ROLE_ADMIN toujours en écriture
        ecriture.addAll(SUPER_ROLES);
        // L'écriture implique la lecture
        lecture.addAll(ecriture);

        document.setRolesLecture(lecture);
        document.setRolesEcriture(ecriture);

        return toResponse(documentRepository.save(document));
    }

    // ── Lister documents accessibles à l'utilisateur ───────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerDocumentsAccessibles(UUID userId,
                                                              Collection<String> userRoles,
                                                              UUID employeId) {
        // ROLE_SE et ROLE_ADMIN voient tout (hors ARCHIVE)
        boolean isSuperAdmin = userRoles.stream().anyMatch(SUPER_ROLES::contains);
        if (isSuperAdmin) {
            return documentRepository.findAll().stream()
                    .filter(d -> d.getStatut() != StatutDocument.ARCHIVE)
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .map(this::toResponse)
                    .toList();
        }
        // Éviter le paramètre null UUID dans la requête PostgreSQL
        if (employeId != null) {
            return documentRepository.findAccessiblesAvecEmploye(userId, userRoles, employeId).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return documentRepository.findAccessibles(userId, userRoles).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse obtenirDocument(UUID id) {
        return toResponse(documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + id)));
    }

    // ── Workflow validation ────────────────────────────────────────────────────

    public DocumentResponse soumettreValidation(UUID id, UUID userId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + id));

        if (!document.isRequiresValidation()) {
            throw new BusinessException("Ce type de document ne nécessite pas de validation");
        }
        if (document.getStatut() != StatutDocument.BROUILLON && document.getStatut() != StatutDocument.REJETE) {
            throw new BusinessException("Seuls les documents en BROUILLON ou REJETE peuvent être soumis");
        }
        if (!document.getCreatedBy().equals(userId)) {
            throw new BusinessException("Seul le créateur du document peut le soumettre");
        }

        document.setStatut(StatutDocument.EN_VALIDATION);
        document.setMotifRejet(null);
        return toResponse(documentRepository.save(document));
    }

    public DocumentResponse validerDocument(UUID id, UUID validateurId) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + id));

        if (document.getStatut() != StatutDocument.EN_VALIDATION) {
            throw new BusinessException("Seuls les documents EN_VALIDATION peuvent être validés");
        }

        document.setStatut(StatutDocument.VALIDE);
        document.setDateValidation(LocalDateTime.now());
        document.setValideParId(validateurId);
        return toResponse(documentRepository.save(document));
    }

    public DocumentResponse rejeterDocument(UUID id, String motif) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + id));

        if (document.getStatut() != StatutDocument.EN_VALIDATION) {
            throw new BusinessException("Seuls les documents EN_VALIDATION peuvent être rejetés");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Un motif de rejet est obligatoire");
        }

        document.setStatut(StatutDocument.REJETE);
        document.setMotifRejet(motif);
        return toResponse(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listerEnValidation() {
        return documentRepository.findByStatutOrderByCreatedAtDesc(StatutDocument.EN_VALIDATION)
                .stream().map(this::toResponse).toList();
    }

    // ── Versions ──────────────────────────────────────────────────────────────

    public VersionDocumentResponse ajouterVersion(UUID documentId, VersionDocumentRequest request, UUID uploadePar) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + documentId));

        if (document.getStatut() == StatutDocument.EN_VALIDATION) {
            throw new BusinessException("Impossible d'ajouter une version : document en cours de validation");
        }

        int nouvelleVersion = document.getVersionActuelle() + 1;
        document.setVersionActuelle(nouvelleVersion);
        documentRepository.save(document);

        VersionDocument version = new VersionDocument();
        version.setDocumentId(documentId);
        version.setVersion(nouvelleVersion);
        version.setUrl(request.url());
        version.setTaille(request.taille());
        version.setTypeFichier(request.typeFichier());
        version.setUploadePar(uploadePar);
        version.setUploadeLe(LocalDateTime.now());

        return toVersionResponse(versionDocumentRepository.save(version));
    }

    @Transactional(readOnly = true)
    public List<VersionDocumentResponse> listerVersions(UUID documentId) {
        return versionDocumentRepository.findByDocumentIdOrderByVersionDesc(documentId)
                .stream().map(this::toVersionResponse).toList();
    }

    // ── Archiver ──────────────────────────────────────────────────────────────

    public DocumentResponse archiverDocument(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + id));
        document.setStatut(StatutDocument.ARCHIVE);
        return toResponse(documentRepository.save(document));
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private DocumentResponse toResponse(Document d) {
        String createdByNom = d.getCreatedBy() == null ? null :
                userRepository.findById(d.getCreatedBy())
                        .map(u -> u.getPrenom() + " " + u.getNom())
                        .orElse(null);

        return new DocumentResponse(
                d.getId(),
                d.getTitre(),
                d.getDescription(),
                d.getTypeDocument(),
                d.getStatut(),
                d.isRequiresValidation(),
                d.getRolesLecture(),
                d.getRolesEcriture(),
                d.getProjetId(),
                d.getConcerneEmployeId(),
                d.getVersionActuelle(),
                d.getMotifRejet(),
                d.getDateValidation(),
                d.getValideParId(),
                d.getCreatedBy(),
                createdByNom,
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
