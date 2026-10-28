package ong.fodde.modules.projets.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Table(name = "demandes_activite")
@Getter
@Setter
@NoArgsConstructor
public class DemandeActivite extends BaseEntity {

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "demandeur_id", nullable = false, columnDefinition = "uuid")
    private UUID demandeurId;

    @Column(name = "titre", length = 255, nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "justification", columnDefinition = "text")
    private String justification;

    @Column(name = "budget_estime")
    private BigDecimal budgetEstime;

    @Column(name = "date_debut_souhaitee")
    private LocalDate dateDebutSouhaitee;

    @Column(name = "date_fin_souhaitee")
    private LocalDate dateFinSouhaitee;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", columnDefinition = "VARCHAR(50)")
    private StatutDemandeActivite statut;

    @Column(name = "valide_par_cp", columnDefinition = "uuid")
    private UUID valideParCp;

    @Column(name = "date_validation_cp")
    private LocalDateTime dateValidationCp;

    @Column(name = "valide_par_rp", columnDefinition = "uuid")
    private UUID valideParRp;

    @Column(name = "date_validation_rp")
    private LocalDateTime dateValidationRp;

    @Column(name = "approuve_par_raf", columnDefinition = "uuid")
    private UUID approuveParRaf;

    @Column(name = "date_approbation_raf")
    private LocalDateTime dateApprobationRaf;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;

    @Column(name = "activite_creee_id", columnDefinition = "uuid")
    private UUID activiteCreeeId;
}
