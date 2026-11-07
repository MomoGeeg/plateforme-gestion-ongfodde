package ong.fodde.modules.financier.service;

import java.io.IOException;
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
import ong.fodde.modules.financier.dto.PieceJustificativeResponse;
import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.PieceJustificative;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.financier.repository.DepenseRepository;
import ong.fodde.modules.financier.repository.PieceJustificativeRepository;
import ong.fodde.modules.ged.service.StockageFichierService;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;
import ong.fodde.shared.securite.AccesPersonnel;

/**
 * Pièces justificatives d'une dépense (facture, reçu…), stockées dans le même
 * espace privé que les documents de la GED.
 */
@Service
@Transactional
public class PieceJustificativeService {

    public record FichierPiece(String nomFichier, String contentType, byte[] contenu) {}

    private static final long TAILLE_MAX_OCTETS = 10L * 1024 * 1024;
    private static final Map<String, String> TYPES = Map.of(
        "pdf", "application/pdf",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "png", "image/png");
    /** Une pièce peut être ajoutée tant que la dépense n'est ni validée ni annulée. */
    private static final Set<StatutDepense> AJOUT_POSSIBLE = Set.of(
        StatutDepense.BROUILLON, StatutDepense.REJETEE, StatutDepense.SOUMISE,
        StatutDepense.VERIFIEE, StatutDepense.EN_APPROBATION_SE);
    /** Une pièce ne peut être retirée que d'une dépense pas encore (ou de nouveau pas) soumise. */
    private static final Set<StatutDepense> RETRAIT_POSSIBLE = Set.of(StatutDepense.BROUILLON, StatutDepense.REJETEE);
    private static final String[] ROLES_FINANCES = {"ROLE_COMPTABLE", "ROLE_RAF", "ROLE_ADMIN"};
    private static final String[] ROLES_LECTURE = {"ROLE_COMPTABLE", "ROLE_RAF", "ROLE_SE", "ROLE_ADMIN"};

    private final PieceJustificativeRepository pieceRepository;
    private final DepenseRepository depenseRepository;
    private final StockageFichierService stockage;
    private final AccesPersonnel accesPersonnel;

    public PieceJustificativeService(PieceJustificativeRepository pieceRepository,
                                     DepenseRepository depenseRepository,
                                     StockageFichierService stockage,
                                     AccesPersonnel accesPersonnel) {
        this.pieceRepository = pieceRepository;
        this.depenseRepository = depenseRepository;
        this.stockage = stockage;
        this.accesPersonnel = accesPersonnel;
    }

    @Transactional(readOnly = true)
    public List<PieceJustificativeResponse> lister(UUID depenseId, User user) {
        Depense depense = depense(depenseId);
        verifierLecture(depense, user);
        return pieceRepository.findByDepenseIdOrderByCreatedAtAsc(depenseId).stream().map(this::versReponse).toList();
    }

    public PieceJustificativeResponse ajouter(UUID depenseId, MultipartFile fichier, User user) {
        Depense depense = depense(depenseId);
        if (!estAuteur(depense, user) && !accesPersonnel.aUnRole(user, ROLES_FINANCES)) {
            throw new AccessDeniedException("Seuls l'auteur de la dépense et les finances peuvent joindre une pièce");
        }
        if (!AJOUT_POSSIBLE.contains(depense.getStatut())) {
            throw new BusinessException("Impossible d'ajouter une pièce à une dépense " + depense.getStatut().name().toLowerCase(Locale.ROOT));
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new BusinessException("Fichier trop volumineux (10 Mo maximum)");
        }
        String extension = extension(fichier.getOriginalFilename());
        if (!TYPES.containsKey(extension)) {
            throw new BusinessException("Format non accepté : PDF, JPG ou PNG uniquement");
        }

        String chemin = "depenses/" + depenseId + "/" + UUID.randomUUID() + "." + extension;
        try {
            stockage.deposer(chemin, fichier.getBytes(), TYPES.get(extension));
        } catch (IOException e) {
            throw new BusinessException("Lecture du fichier impossible");
        }

        PieceJustificative piece = new PieceJustificative();
        piece.setDepenseId(depenseId);
        piece.setNom(nomPropre(fichier.getOriginalFilename()));
        piece.setUrl(chemin);
        piece.setTypeFichier(extension);
        piece.setTaille(fichier.getSize());
        return versReponse(pieceRepository.save(piece));
    }

    @Transactional(readOnly = true)
    public FichierPiece telecharger(UUID depenseId, UUID pieceId, User user) {
        Depense depense = depense(depenseId);
        verifierLecture(depense, user);
        PieceJustificative piece = piece(depenseId, pieceId);
        String extension = piece.getTypeFichier() != null ? piece.getTypeFichier() : extension(piece.getUrl());
        return new FichierPiece(piece.getNom(), TYPES.getOrDefault(extension, "application/octet-stream"),
            stockage.lire(piece.getUrl()));
    }

    public void supprimer(UUID depenseId, UUID pieceId, User user) {
        Depense depense = depense(depenseId);
        if (!estAuteur(depense, user) && !accesPersonnel.aUnRole(user, "ROLE_RAF", "ROLE_ADMIN")) {
            throw new AccessDeniedException("Seul l'auteur de la dépense peut retirer une pièce");
        }
        if (!RETRAIT_POSSIBLE.contains(depense.getStatut())) {
            throw new BusinessException("Une pièce ne peut être retirée que d'un brouillon ou d'une dépense rejetée");
        }
        // Le fichier reste dans le stockage (traçabilité) ; seule la référence est retirée.
        pieceRepository.delete(piece(depenseId, pieceId));
    }

    public long compter(UUID depenseId) {
        return pieceRepository.countByDepenseId(depenseId);
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────

    private void verifierLecture(Depense depense, User user) {
        if (!estAuteur(depense, user) && !accesPersonnel.aUnRole(user, ROLES_LECTURE)) {
            throw new AccessDeniedException("Pièces réservées aux finances, à la direction et à l'auteur");
        }
    }

    private static boolean estAuteur(Depense depense, User user) {
        return user.getId().equals(depense.getSaisiParId());
    }

    private Depense depense(UUID id) {
        return depenseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable : " + id));
    }

    private PieceJustificative piece(UUID depenseId, UUID pieceId) {
        PieceJustificative piece = pieceRepository.findById(pieceId)
            .orElseThrow(() -> new ResourceNotFoundException("Pièce introuvable : " + pieceId));
        if (!piece.getDepenseId().equals(depenseId)) {
            throw new ResourceNotFoundException("Pièce introuvable pour cette dépense");
        }
        return piece;
    }

    private PieceJustificativeResponse versReponse(PieceJustificative p) {
        return new PieceJustificativeResponse(p.getId(), p.getDepenseId(), p.getNom(), p.getTypeFichier(), p.getTaille(),
            p.getCreatedAt());
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
