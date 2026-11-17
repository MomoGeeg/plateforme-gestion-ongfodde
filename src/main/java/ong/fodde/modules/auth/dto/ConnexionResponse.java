package ong.fodde.modules.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.auth.entity.Connexion;

/**
 * Une ligne de l'historique des connexions.
 * Fin de session ({@code finSession}, {@code typeFin}) :
 * - DECONNEXION : la personne a cliqué sur « Se déconnecter » ;
 * - EXPIRATION : pas de déconnexion, le jeton a expiré (fin = connexion + durée du jeton) ;
 * - vide : session encore ouverte (ou connexion refusée).
 */
public record ConnexionResponse(
        UUID id,
        UUID userId,
        String email,
        String nomComplet,
        boolean succes,
        String motifEchec,
        String adresseIp,
        String navigateur,
        LocalDateTime dateConnexion,
        LocalDateTime finSession,
        String typeFin) {

    public static ConnexionResponse de(Connexion c, long dureeJetonMs, LocalDateTime maintenant) {
        LocalDateTime fin = null;
        String type = null;
        if (c.isSucces()) {
            LocalDateTime expiration = c.getDateConnexion().plusNanos(dureeJetonMs * 1_000_000L);
            if (c.getDateDeconnexion() != null && c.getDateDeconnexion().isBefore(expiration)) {
                fin = c.getDateDeconnexion();
                type = "DECONNEXION";
            } else if (!maintenant.isBefore(expiration)) {
                fin = expiration;
                type = "EXPIRATION";
            }
        }
        return new ConnexionResponse(c.getId(), c.getUserId(), c.getEmail(), c.getNomComplet(), c.isSucces(),
                c.getMotifEchec(), c.getAdresseIp(), c.getNavigateur(), c.getDateConnexion(), fin, type);
    }
}
