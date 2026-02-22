package ong.fodde.modules.financier.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lignes_budgetaires")
@Getter
@Setter
@NoArgsConstructor
public class LigneBudgetaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "budget_id", nullable = false, columnDefinition = "uuid")
    private UUID budgetId;
    

    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "intitule", length = 300, nullable = false)
    private String intitule;

    @Column(name = "categorie", length = 100)
    private String categorie;

    @Column(name = "montant_prevu", nullable = false)
    private BigDecimal montantPrevu;

    @Column(name = "montant_engage")
    private BigDecimal montantEngage = BigDecimal.ZERO;

    @Column(name = "montant_depense")
    private BigDecimal montantDepense = BigDecimal.ZERO;

    @Column(name = "est_gelee", nullable = false)
    private Boolean estGelee = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
