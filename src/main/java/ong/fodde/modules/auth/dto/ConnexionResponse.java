package ong.fodde.modules.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.auth.entity.Connexion;

public record ConnexionResponse(
        UUID id,
        UUID userId,
        String email,
        String nomComplet,
        boolean succes,
        String motifEchec,
        String adresseIp,
        String navigateur,
        LocalDateTime dateConnexion) {

    public static ConnexionResponse de(Connexion c) {
        return new ConnexionResponse(c.getId(), c.getUserId(), c.getEmail(), c.getNomComplet(), c.isSucces(),
                c.getMotifEchec(), c.getAdresseIp(), c.getNavigateur(), c.getDateConnexion());
    }
}
