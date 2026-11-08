package ong.fodde.modules.missions.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ligne du budget d'une mission : quantité × prix unitaire (ex. per diem 3 agents × 4 jours). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class FraisMission {

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 20)
    private CategorieFrais categorie;

    @Column(name = "libelle", length = 200)
    private String libelle;

    @Column(name = "quantite", nullable = false)
    private BigDecimal quantite;

    @Column(name = "prix_unitaire", nullable = false)
    private BigDecimal prixUnitaire;

    public FraisMission(CategorieFrais categorie, String libelle, BigDecimal quantite, BigDecimal prixUnitaire) {
        this.categorie = categorie;
        this.libelle = libelle;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal montant() {
        return quantite.multiply(prixUnitaire);
    }
}
