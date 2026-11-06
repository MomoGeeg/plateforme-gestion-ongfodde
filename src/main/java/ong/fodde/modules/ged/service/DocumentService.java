package ong.fodde.modules.ged.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.ged.entity.TelechargementLog;
import ong.fodde.modules.ged.repository.TelechargementLogRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
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

    /** Préfixe des chemins de fichiers réellement stockés (les anciennes versions ne contiennent qu'un nom). */
    private static final String PREFIXE_STOCKAGE = "documents/";

    private static final long TAILLE_MAX_OCTETS = 25L * 1024 * 1024;

    /** Extension autorisée → type de fichier affiché */
    private static final Map<String, String> EXTENSIONS_AUTORISEES = Map.of(
            "pdf", "PDF",
            "doc", "DOCX", "docx", "DOCX",
            "xls", "XLSX", "xlsx", "XLSX",
            "jpg", "JPG", "jpeg", "JPG",
            "png", "PNG",
            "zip", "ZIP");

    private final DocumentRepository documentRepository;
    private final VersionDocumentRepository versionDocumentRepository;
    private final UserRepository userRepository;
    private final TelechargementLogRepository telechargementLogRepository;
    private final EmployeRepository employeRepository;
    private final StockageFichierService stockageFichierService;

    public DocumentService(DocumentRepository documentRepository,
                           VersionDocumentRepository versionDocumentRepository,
                           UserRepository userRepository,
                           TelechargementLogRepository telechargementLogRepository,
                           EmployeRepository employeRepository,
                           StockageFichierService stockageFichierService) {
        this.documentRepository = documentRepository;
        this.versionDocumentRepository = versionDocumentRepository;
        this.userRepository = userRepository;
        this.telechargementLogRepository = telechargementLogRepository;
        this.employeRepository = employeRepository;
        this.stockageFichierService = stockageFichierService;
    }

    /** Fichier prêt à être renvoyé au navigateur. */
    public record FichierTelecharge(String nomFichier, String contentType, byte[] contenu) {
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

    /**
     * Dépose un vrai fichier : envoi vers le stockage privé puis création d'une nouvelle version.
     */
    public VersionDocumentResponse deposerFichier(UUID documentId, MultipartFile fichier, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + documentId));

        if (!peutEcrire(document, user)) {
            throw new AccessDeniedException("Vous n'avez pas le droit de déposer un fichier sur ce document");
        }
        if (document.getStatut() == StatutDocument.EN_VALIDATION) {
            throw new BusinessException("Impossible d'ajouter une version : document en cours de validation");
        }
        if (document.getStatut() == StatutDocument.ARCHIVE) {
            throw new BusinessException("Impossible d'ajouter une version : document archivé");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new BusinessException("Fichier trop volumineux (25 Mo maximum)");
        }
        String extension = extension(fichier.getOriginalFilename());
        String typeFichier = EXTENSIONS_AUTORISEES.get(extension);
        if (typeFichier == null) {
            throw new BusinessException("Format non autorisé. Formats acceptés : PDF, DOCX, XLSX, JPG, PNG, ZIP");
        }

        int nouvelleVersion = document.getVersionActuelle() + 1;
        // Nom aléatoire : jamais de nom fourni par l'utilisateur dans le chemin de stockage
        String chemin = PREFIXE_STOCKAGE + documentId + "/v" + nouvelleVersion + "-" + UUID.randomUUID() + "." + extension;

        try {
            stockageFichierService.deposer(chemin, fichier.getBytes(), fichier.getContentType());
        } catch (IOException e) {
            throw new BusinessException("Lecture du fichier impossible");
        }

        document.setVersionActuelle(nouvelleVersion);
        documentRepository.save(document);

        VersionDocument version = new VersionDocument();
        version.setDocumentId(documentId);
        version.setVersion(nouvelleVersion);
        version.setUrl(chemin);
        version.setTaille(fichier.getSize());
        version.setTypeFichier(typeFichier);
        version.setUploadePar(user.getId());
        version.setUploadeLe(LocalDateTime.now());

        return toVersionResponse(versionDocumentRepository.save(version));
    }

    /**
     * Télécharge le fichier d'une version après contrôle des droits de lecture, et journalise l'accès.
     */
    public FichierTelecharge telechargerFichier(UUID documentId, UUID versionId, User user, String ipAddress) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document non trouvé : " + documentId));
        VersionDocument version = versionDocumentRepository.findById(versionId)
                .filter(v -> v.getDocumentId().equals(documentId))
                .orElseThrow(() -> new ResourceNotFoundException("Version non trouvée : " + versionId));

        if (!peutLire(document, user)) {
            throw new AccessDeniedException("Vous n'avez pas accès à ce document");
        }
        if (!estStocke(version)) {
            throw new BusinessException(
                    "Aucun fichier stocké pour cette version (dépôt antérieur à la mise en place du stockage)");
        }

        byte[] contenu = stockageFichierService.lire(version.getUrl());

        TelechargementLog log = new TelechargementLog();
        log.setDocumentId(documentId);
        log.setUserId(user.getId());
        log.setDateTel(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        telechargementLogRepository.save(log);

        String extension = extension(version.getUrl());
        String nomFichier = nettoyerNom(document.getTitre()) + "-v" + version.getVersion() + "." + extension;
        return new FichierTelecharge(nomFichier, contentTypePour(extension), contenu);
    }

    // ── Droits d'accès ────────────────────────────────────────────────────────

    private boolean estSuperAdmin(User user) {
        return rolesDe(user).stream().anyMatch(SUPER_ROLES::contains);
    }

    private boolean peutEcrire(Document document, User user) {
        if (estSuperAdmin(user) || user.getId().equals(document.getCreatedBy())) {
            return true;
        }
        Set<String> roles = rolesDe(user);
        return document.getRolesEcriture().stream().anyMatch(roles::contains);
    }

    private boolean peutLire(Document document, User user) {
        if (estSuperAdmin(user)) {
            return true;
        }
        // Mêmes règles que la liste des documents : les documents archivés ne sont visibles que des super-admins
        if (document.getStatut() == StatutDocument.ARCHIVE) {
            return false;
        }
        if (user.getId().equals(document.getCreatedBy()) || peutEcrire(document, user)) {
            return true;
        }
        Set<String> roles = rolesDe(user);
        if (document.getRolesLecture().stream().anyMatch(roles::contains)) {
            return true;
        }
        // Document personnel (bulletin, contrat, évaluation) : l'employé concerné y a accès
        return document.getConcerneEmployeId() != null
                && employeRepository.findByUserId(user.getId())
                        .map(e -> document.getConcerneEmployeId().equals(e.getId()))
                        .orElse(false);
    }

    private static Set<String> rolesDe(User user) {
        Set<String> roles = new HashSet<>();
        user.getRoles().forEach(r -> roles.add(r.getNom()));
        return roles;
    }

    // ── Utilitaires fichiers ──────────────────────────────────────────────────

    private static boolean estStocke(VersionDocument version) {
        return version.getUrl() != null && version.getUrl().startsWith(PREFIXE_STOCKAGE);
    }

    private static String extension(String nom) {
        if (nom == null) {
            return "";
        }
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }

    private static String nettoyerNom(String titre) {
        String nom = titre == null ? "document" : titre.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        return nom.isEmpty() ? "document" : nom;
    }

    private static String contentTypePour(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
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
                v.getUploadeLe(),
                estStocke(v)
        );
    }
}
