package ong.fodde.modules.projets.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

/** Liste de présence (CSV) ou photo d'une activité, stockée dans l'espace privé des fichiers. */
@Entity
@Table(name = "pieces_activite")
@Getter
@Setter
@NoArgsConstructor
public class PieceActivite extends BaseEntity {

    @Column(name = "activite_id", nullable = false, columnDefinition = "uuid")
    private UUID activiteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_piece", nullable = false, length = 20)
    private TypePiece type;

    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    @Column(name = "chemin", nullable = false, columnDefinition = "text")
    private String chemin;

    @Column(name = "type_fichier", length = 20)
    private String typeFichier;

    @Column(name = "taille")
    private Long taille;

    @Column(name = "ajoute_par", columnDefinition = "uuid")
    private UUID ajoutePar;
}
