package ong.fodde.modules.projets.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

/** Valeur d'un indicateur du projet relevée sur un périmètre, à une date. */
@Entity
@Table(name = "mesures_perimetre")
@Getter
@Setter
@NoArgsConstructor
public class MesurePerimetre extends BaseEntity {

    @Column(name = "indicateur_id", nullable = false, columnDefinition = "uuid")
    private UUID indicateurId;

    @Column(name = "perimetre_id", nullable = false, columnDefinition = "uuid")
    private UUID perimetreId;

    @Column(name = "date_mesure", nullable = false)
    private LocalDate dateMesure;

    @Column(name = "valeur", nullable = false)
    private BigDecimal valeur;

    @Column(name = "commentaire", columnDefinition = "text")
    private String commentaire;

    @Column(name = "saisi_par", columnDefinition = "uuid")
    private UUID saisiPar;
}
