package ong.fodde.modules.se.dto;

import java.util.UUID;

public record ChampFormulaireResponse(
        UUID id,
        UUID formulaireId,
        String libelle,
        String typeChamp,
        Boolean obligatoire,
        Integer ordre,
        String options
) {
}
