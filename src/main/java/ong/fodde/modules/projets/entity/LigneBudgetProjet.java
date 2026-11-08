package ong.fodde.modules.projets.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ligne du budget prévisionnel d'un projet, sur toute sa durée. C'est la référence dont le RAF
 * tire chaque année l'allocation du budget annuel.
 */
@Entity
@Table(name = "lignes_budget_projet")
@Getter
@Setter
@NoArgsConstructor
public class LigneBudgetProjet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "code", length = 30)
    private String code;

    @Column(name = "intitule", nullable = false, length = 300)
    private String intitule;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 30)
    private CategorieBudget categorie;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    /** Convention (financement d'un bailleur) qui couvre la ligne, si elle est connue. */
    @Column(name = "financement_id", columnDefinition = "uuid")
    private UUID financementId;

    @Column(name = "ordre", nullable = false)
    private Integer ordre = 1;
}
