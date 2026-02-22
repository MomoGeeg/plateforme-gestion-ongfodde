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
@Table(name = "depenses")
@Getter
@Setter
@NoArgsConstructor
public class Depense extends BaseEntity {

    @Column(name = "numero_piece", length = 30, unique = true)
    private String numeroPiece;

    @Column(name = "ligne_budgetaire_id", nullable = false, columnDefinition = "uuid")
    private UUID ligneBudgetaireId;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "libelle", length = 300, nullable = false)
    private String libelle;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "devise", nullable = false, columnDefinition = "devise")
    private Devise devise;

    @Column(name = "taux_change")
    private BigDecimal tauxChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false, columnDefinition = "mode_paiement")
    private ModePaiement modePaiement;

    @Column(name = "beneficiaire", length = 200, nullable = false)
    private String beneficiaire;

    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_depense")
    private StatutDepense statut = StatutDepense.BROUILLON;

    @Column(name = "saisie_par", nullable = false, columnDefinition = "uuid")
    private UUID saisiParId;

    @Column(name = "verifie_par", columnDefinition = "uuid")
    private UUID verifiePar;

    @Column(name = "valide_par", columnDefinition = "uuid")
    private UUID validePar;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;
}
