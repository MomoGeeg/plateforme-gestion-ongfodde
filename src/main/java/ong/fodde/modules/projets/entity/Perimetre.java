package ong.fodde.modules.projets.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

/**
 * Périmètre maraîcher : situé dans un village, lié à un seul projet, suivi par un technicien
 * terrain et son superviseur. Créé par le chef de projet (coordonnateur) ou le RP.
 */
@Entity
@Table(name = "perimetres")
@Getter
@Setter
@NoArgsConstructor
public class Perimetre extends BaseEntity {

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "nom", length = 200, nullable = false)
    private String nom;

    @Column(name = "village", length = 200, nullable = false)
    private String village;

    @Column(name = "commune", length = 150)
    private String commune;

    @Column(name = "departement", length = 150)
    private String departement;

    @Column(name = "superficie_ha")
    private BigDecimal superficieHa;

    @Column(name = "technicien_id", columnDefinition = "uuid")
    private UUID technicienId;

    @Column(name = "superviseur_id", columnDefinition = "uuid")
    private UUID superviseurId;

    /** Coordonnées géographiques (degrés décimaux, WGS 84). */
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    /** Cultures pratiquées, séparées par des virgules (oignon, tomate…). */
    @Column(name = "cultures", length = 500)
    private String cultures;
}
