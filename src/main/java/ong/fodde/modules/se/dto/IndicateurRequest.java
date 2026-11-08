package ong.fodde.modules.se.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.se.entity.NiveauIndicateur;
import ong.fodde.modules.se.entity.TypeIndicateur;

public record IndicateurRequest(
    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    /** Facultatif : généré à partir du code du projet s'il est vide (ex. PROJ-2025-001-IND-03). */

    String code,

    @NotBlank(message = "Le libellé est obligatoire")
    String libelle,

    @NotNull(message = "Le type d'indicateur est obligatoire")
    TypeIndicateur type,

    @NotNull(message = "Le niveau d'indicateur est obligatoire")
    NiveauIndicateur niveau,

    String unite,

    BigDecimal valeurBaseline,

    BigDecimal valeurCible,

    String frequenceCollecte,

    String sourceVerification,

    UUID responsableId,

    /** Résultat attendu mesuré (facultatif), du même projet. */
    UUID resultatId
) {}
