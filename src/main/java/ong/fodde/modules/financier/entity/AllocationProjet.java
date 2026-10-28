package ong.fodde.modules.financier.entity;

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
@Table(name = "allocations_projets")
@Getter
@Setter
@NoArgsConstructor
public class AllocationProjet extends BaseEntity {

    @Column(name = "budget_annuel_id", nullable = false, columnDefinition = "uuid")
    private UUID budgetAnnuelId;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "montant_alloue", nullable = false)
    private BigDecimal montantAlloue;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAllocation statut = StatutAllocation.BROUILLON;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;

    @Column(name = "soumis_par_id", columnDefinition = "uuid")
    private UUID soumisParId;

    @Column(name = "valide_par_id", columnDefinition = "uuid")
    private UUID valideParId;

    @Column(name = "date_validation")
    private LocalDate dateValidation;
}
