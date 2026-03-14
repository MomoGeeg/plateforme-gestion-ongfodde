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
@Table(name = "budgets_annuels")
@Getter
@Setter
@NoArgsConstructor
public class BudgetAnnuel extends BaseEntity {

    @Column(name = "exercice", nullable = false)
    private Integer exercice;

    @Column(name = "intitule", length = 300)
    private String intitule;

    @Column(name = "montant_total", nullable = false)
    private BigDecimal montantTotal;

    @Column(name = "devise", length = 10, nullable = false)
    private String devise = "XOF";

    @Column(name = "date_vote")
    private LocalDate dateVote;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutBudgetAnnuel statut = StatutBudgetAnnuel.BROUILLON;

    @Column(name = "saisi_par_id", columnDefinition = "uuid")
    private UUID saisiParId;
}
