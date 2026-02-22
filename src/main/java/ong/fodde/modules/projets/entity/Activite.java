package ong.fodde.modules.projets.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
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

@Entity
@Table(name = "activites")
@Getter
@Setter
@NoArgsConstructor
public class Activite extends BaseEntity {

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "parent_id", columnDefinition = "uuid")
    private UUID parentId;

    @Column(name = "titre", length = 300, nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "responsable_id", columnDefinition = "uuid")
    private UUID responsableId;

    @Column(name = "date_prevu_debut", nullable = false)
    private LocalDate datePrevuDebut;

    @Column(name = "date_prevu_fin", nullable = false)
    private LocalDate datePrevuFin;

    @Column(name = "date_reel_debut")
    private LocalDate dateReelDebut;

    @Column(name = "date_reel_fin")
    private LocalDate dateReelFin;

    @Column(name = "budget_alloue")
    private BigDecimal budgetAlloue;

    @Column(name = "taux_avancement", nullable = false)
    private Integer tauxAvancement = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_activite")
    private StatutActivite statut = StatutActivite.PLANIFIEE;
}
