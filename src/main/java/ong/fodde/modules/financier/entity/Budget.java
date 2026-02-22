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
import ong.fodde.modules.projets.entity.Devise;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
public class Budget extends BaseEntity {

    @Column(name = "projet_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "montant_total", nullable = false)
    private BigDecimal montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "devise", nullable = false, columnDefinition = "devise")
    private Devise devise;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_budget")
    private StatutBudget statut = StatutBudget.ACTIF;

    @Column(name = "date_validation")
    private LocalDate dateValidation;

    @Column(name = "valide_par", columnDefinition = "uuid")
    private UUID valideParId;
}
