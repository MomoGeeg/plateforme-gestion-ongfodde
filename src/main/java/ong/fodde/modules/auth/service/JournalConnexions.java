package ong.fodde.modules.auth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.dto.ConnexionResponse;
import ong.fodde.modules.auth.entity.Connexion;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.ConnexionRepository;
import ong.fodde.shared.exception.ResourceNotFoundException;

/** Journal des tentatives de connexion. */
@Service
public class JournalConnexions {

    private static final Logger log = LoggerFactory.getLogger(JournalConnexions.class);

    public static final String MOT_DE_PASSE = "MOT_DE_PASSE";
    public static final String COMPTE_INACTIF = "COMPTE_INACTIF";
    public static final String COMPTE_VERROUILLE = "COMPTE_VERROUILLE";
    public static final String COMPTE_INCONNU = "COMPTE_INCONNU";

    private final ConnexionRepository repository;
    private final long dureeJetonMs;

    public JournalConnexions(ConnexionRepository repository,
                             @Value("${app.jwt.expiration:86400000}") long dureeJetonMs) {
        this.repository = repository;
        this.dureeJetonMs = dureeJetonMs;
    }

    /**
     * Transaction séparée : un refus de connexion lève une exception qui annule la transaction
     * de la connexion ; la ligne du journal doit être gardée quand même.
     * Une panne du journal ne doit jamais empêcher quelqu'un de se connecter.
     * @return l'identifiant de la ligne (renvoyé au navigateur pour la déconnexion), ou null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID enregistrer(String email, User user, boolean succes, String motifEchec,
                            String adresseIp, String navigateur) {
        try {
            Connexion c = new Connexion();
            c.setEmail(tronquer(email == null ? "" : email.trim(), 255));
            if (user != null) {
                c.setUserId(user.getId());
                c.setNomComplet(tronquer(user.getPrenom() + " " + user.getNom(), 200));
            }
            c.setSucces(succes);
            c.setMotifEchec(succes ? null : motifEchec);
            c.setAdresseIp(tronquer(adresseIp, 64));
            c.setNavigateur(tronquer(navigateur, 300));
            c.setDateConnexion(LocalDateTime.now());
            return repository.save(c).getId();
        } catch (RuntimeException e) {
            log.warn("Connexion non journalisée pour {} : {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * Note l'heure de déconnexion. Sans effet si la ligne n'appartient pas à la personne
     * connectée, si elle est déjà fermée, ou si l'identifiant est inconnu.
     */
    @Transactional
    public void deconnecter(UUID connexionId, UUID userId) {
        if (connexionId == null || userId == null) return;
        repository.findById(connexionId)
                .filter(c -> c.isSucces() && userId.equals(c.getUserId()) && c.getDateDeconnexion() == null)
                .ifPresent(c -> c.setDateDeconnexion(LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public List<ConnexionResponse> lister(UUID userId, int jours) {
        LocalDateTime depuis = LocalDateTime.now().minusDays(Math.max(1, Math.min(jours, 366)));
        List<Connexion> lignes = userId == null
                ? repository.findTop1000ByDateConnexionGreaterThanEqualOrderByDateConnexionDesc(depuis)
                : repository.findTop1000ByUserIdAndDateConnexionGreaterThanEqualOrderByDateConnexionDesc(userId, depuis);
        LocalDateTime maintenant = LocalDateTime.now();
        return lignes.stream().map(c -> ConnexionResponse.de(c, dureeJetonMs, maintenant)).toList();
    }

    /**
     * Vide l'historique : les lignes de plus de {@code jours} jours, ou tout l'historique si
     * {@code jours} vaut 0. Les sessions en cours supprimées ne recevront pas d'heure de déconnexion.
     * @return le nombre de lignes supprimées
     */
    @Transactional
    public int supprimer(int jours, UUID adminId) {
        if (jours < 0) {
            throw new IllegalArgumentException("Nombre de jours négatif");
        }
        LocalDateTime limite = jours == 0
                ? LocalDateTime.now().plusMinutes(1)
                : LocalDateTime.now().minusDays(jours);
        int n = repository.supprimerAvant(limite);
        log.info("Historique des connexions : {} ligne(s) supprimée(s) (plus de {} jours) par {}", n, jours, adminId);
        return n;
    }

    /** Supprime une connexion de l'historique. */
    @Transactional
    public void supprimerUne(UUID connexionId, UUID adminId) {
        if (!repository.existsById(connexionId)) {
            throw new ResourceNotFoundException("Connexion introuvable dans l'historique.");
        }
        repository.deleteById(connexionId);
        log.info("Historique des connexions : ligne {} supprimée par {}", connexionId, adminId);
    }

    static String tronquer(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
