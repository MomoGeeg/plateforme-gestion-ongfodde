package ong.fodde.modules.projets.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.ged.service.StockageFichierService;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.dto.AnimateurDtos.PieceResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.PieceActivite;
import ong.fodde.modules.projets.entity.TypePiece;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.PieceActiviteRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Pièces d'une activité : une liste de présence au format CSV, qui donne le nombre de
 * participants, et jusqu'à trois photos. Stockées dans l'espace privé des fichiers.
 */
@Service
@Transactional
public class PieceActiviteService {

    public record Fichier(String nom, String contentType, byte[] contenu) {
    }

    static final int PHOTOS_MAX = 3;
    private static final long TAILLE_LISTE_MAX = 2L * 1024 * 1024;
    private static final long TAILLE_PHOTO_MAX = 5L * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
            "csv", "text/csv",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");
    /** Les listes de présence contiennent des noms : lecture réservée au suivi du projet. */
    private static final String[] ROLES_LECTURE = {"ROLE_SE", "ROLE_RESPONSABLE_SE", "ROLE_ADMIN"};

    private final PieceActiviteRepository pieceRepository;
    private final PerimetreRepository perimetreRepository;
    private final ActiviteService activiteService;
    private final StockageFichierService stockage;
    private final DroitsProjet droits;

    public PieceActiviteService(PieceActiviteRepository pieceRepository, PerimetreRepository perimetreRepository,
                                ActiviteService activiteService, StockageFichierService stockage, DroitsProjet droits) {
        this.pieceRepository = pieceRepository;
        this.perimetreRepository = perimetreRepository;
        this.activiteService = activiteService;
        this.stockage = stockage;
        this.droits = droits;
    }

    @Transactional(readOnly = true)
    public List<PieceResponse> lister(UUID activiteId, User user) {
        Activite activite = activiteService.activiteExistante(activiteId);
        verifierLecture(activite, user);
        return pieceRepository.findByActiviteIdAndIsDeletedFalseOrderByCreatedAtAsc(activiteId).stream()
                .map(PieceActiviteService::versReponse).toList();
    }

    /** Une seule liste par activité : la nouvelle remplace l'ancienne et refait le comptage. */
    public ActiviteResponse deposerListePresence(UUID activiteId, MultipartFile fichier, User user) {
        Activite activite = activiteService.activiteExistante(activiteId);
        verifierEcriture(activite, user);
        String extension = verifierFichier(fichier, TAILLE_LISTE_MAX);
        if (!"csv".equals(extension)) {
            throw new BusinessException("La liste de présence doit être un fichier CSV");
        }
        byte[] contenu = lire(fichier);
        ListePresence.Comptage comptage = ListePresence.compter(contenu);
        pieceRepository.findByActiviteIdAndTypeAndIsDeletedFalse(activiteId, TypePiece.LISTE_PRESENCE)
                .forEach(p -> {
                    p.setIsDeleted(true);
                    pieceRepository.save(p);
                });
        enregistrer(activite, TypePiece.LISTE_PRESENCE, fichier, extension, contenu, user);
        activiteService.appliquerListePresence(activite, comptage);
        return activiteService.toResponse(activite);
    }

    public PieceResponse ajouterPhoto(UUID activiteId, MultipartFile fichier, User user) {
        Activite activite = activiteService.activiteExistante(activiteId);
        verifierEcriture(activite, user);
        String extension = verifierFichier(fichier, TAILLE_PHOTO_MAX);
        if ("csv".equals(extension)) {
            throw new BusinessException("Une photo doit être au format JPG ou PNG");
        }
        if (pieceRepository.countByActiviteIdAndTypeAndIsDeletedFalse(activiteId, TypePiece.PHOTO) >= PHOTOS_MAX) {
            throw new BusinessException("Trois photos au plus par activité : retirez-en une d'abord");
        }
        return versReponse(enregistrer(activite, TypePiece.PHOTO, fichier, extension, lire(fichier), user));
    }

    /** Retrait d'une photo (le fichier reste dans le stockage, pour la traçabilité). */
    public void retirerPhoto(UUID activiteId, UUID pieceId, User user) {
        Activite activite = activiteService.activiteExistante(activiteId);
        verifierEcriture(activite, user);
        PieceActivite piece = piece(activiteId, pieceId);
        if (piece.getType() != TypePiece.PHOTO) {
            throw new BusinessException("La liste de présence se remplace par une nouvelle, elle ne se retire pas");
        }
        piece.setIsDeleted(true);
        pieceRepository.save(piece);
    }

    @Transactional(readOnly = true)
    public Fichier telecharger(UUID activiteId, UUID pieceId, User user) {
        Activite activite = activiteService.activiteExistante(activiteId);
        verifierLecture(activite, user);
        PieceActivite piece = piece(activiteId, pieceId);
        return new Fichier(piece.getNom(), TYPES.getOrDefault(piece.getTypeFichier(), "application/octet-stream"),
                stockage.lire(piece.getChemin()));
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────

    private PieceActivite enregistrer(Activite activite, TypePiece type, MultipartFile fichier, String extension,
                                      byte[] contenu, User user) {
        String dossier = type == TypePiece.PHOTO ? "photos" : "presence";
        String chemin = "activites/" + activite.getId() + "/" + dossier + "/" + UUID.randomUUID() + "." + extension;
        stockage.deposer(chemin, contenu, TYPES.get(extension));
        PieceActivite piece = new PieceActivite();
        piece.setActiviteId(activite.getId());
        piece.setType(type);
        piece.setNom(nomPropre(fichier.getOriginalFilename()));
        piece.setChemin(chemin);
        piece.setTypeFichier(extension);
        piece.setTaille(fichier.getSize());
        piece.setAjoutePar(user.getId());
        return pieceRepository.save(piece);
    }

    private void verifierEcriture(Activite activite, User user) {
        if (!activiteService.peutRenseigner(activite, user)) {
            throw new AccessDeniedException("Seuls le responsable de l'activité, le chef de projet et le RP y joignent des pièces");
        }
    }

    private void verifierLecture(Activite activite, User user) {
        if (activiteService.peutRenseigner(activite, user) || droits.aUnRole(user, ROLES_LECTURE)) {
            return;
        }
        boolean superviseur = activite.getPerimetreId() != null && perimetreRepository.findById(activite.getPerimetreId())
                .map(p -> user.getId().equals(p.getSuperviseurId())).orElse(false);
        if (!superviseur) {
            throw new AccessDeniedException("Pièces réservées au suivi de l'activité");
        }
    }

    private static String verifierFichier(MultipartFile fichier, long tailleMax) {
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        if (fichier.getSize() > tailleMax) {
            throw new BusinessException("Fichier trop volumineux (" + tailleMax / (1024 * 1024) + " Mo maximum)");
        }
        String extension = extension(fichier.getOriginalFilename());
        if (!TYPES.containsKey(extension)) {
            throw new BusinessException("Format non accepté : CSV pour la liste, JPG ou PNG pour les photos");
        }
        return extension;
    }

    private static byte[] lire(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Lecture du fichier impossible");
        }
    }

    private PieceActivite piece(UUID activiteId, UUID pieceId) {
        return pieceRepository.findById(pieceId)
                .filter(p -> p.getActiviteId().equals(activiteId) && !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Pièce introuvable pour cette activité"));
    }

    private static PieceResponse versReponse(PieceActivite p) {
        return new PieceResponse(p.getId(), p.getType(), p.getNom(), p.getTypeFichier(), p.getTaille(), p.getCreatedAt());
    }

    private static String extension(String nom) {
        if (nom == null) {
            return "";
        }
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }

    private static String nomPropre(String nom) {
        String n = nom == null ? "piece" : nom.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        if (n.isEmpty()) {
            n = "piece";
        }
        return n.length() > 255 ? n.substring(n.length() - 255) : n;
    }
}
