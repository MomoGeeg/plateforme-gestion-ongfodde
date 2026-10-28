package ong.fodde.modules.se.entity;

import java.math.BigDecimal;
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
@Table(name = "indicateurs")
@Getter
@Setter
@NoArgsConstructor
public class Indicateur extends BaseEntity {

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    @Column(name = "libelle", nullable = false, columnDefinition = "text")
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "type_indicateur")
    private TypeIndicateur type;

    @Enumerated(EnumType.STRING)
    @Column(name = "niveau", nullable = false, columnDefinition = "niveau_indicateur")
    private NiveauIndicateur niveau;

    @Column(name = "unite", length = 50)
    private String unite;

    @Column(name = "valeur_baseline")
    private BigDecimal valeurBaseline;

    @Column(name = "valeur_cible")
    private BigDecimal valeurCible;

    @Column(name = "valeur_atteinte")
    private BigDecimal valeurAtteinte = BigDecimal.ZERO;

    @Column(name = "frequence_collecte", length = 50)
    private String frequenceCollecte;

    @Column(name = "source_verification", columnDefinition = "text")
    private String sourceVerification;

    @Column(name = "responsable_id", columnDefinition = "uuid")
    private UUID responsableId;
}
