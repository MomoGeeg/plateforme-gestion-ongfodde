package ong.fodde.modules.se.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChampFormulaireRequest(
        @NotNull(message = "L'identifiant du formulaire est obligatoire")
        UUID formulaireId,

        @NotBlank(message = "Le libellé est obligatoire")
        String libelle,

        @NotNull(message = "Le type de champ est obligatoire")
        String typeChamp,

        Boolean obligatoire,
        Integer ordre,
        String options
) {
}
