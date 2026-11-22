package ong.fodde.modules.projets.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.entity.TypePiece;

/** Animateurs des projets et pièces des activités. */
public final class AnimateurDtos {

    private AnimateurDtos() {
    }

    public record AffectationRequest(@NotNull UUID animateurId) {
    }

    public record AnimateurResponse(UUID id, String nom) {
    }

    /** Projet d'un animateur, avec son coordonnateur. */
    public record ProjetAnimateur(UUID id, String code, String titre, StatutProjet statut, String coordonnateurNom) {
    }

    public record PieceResponse(UUID id, TypePiece type, String nom, String typeFichier, Long taille,
                                LocalDateTime createdAt) {
    }
}
