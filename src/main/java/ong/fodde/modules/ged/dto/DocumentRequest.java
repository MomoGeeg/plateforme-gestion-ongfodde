package ong.fodde.modules.ged.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.ged.entity.TypeDocument;
import ong.fodde.modules.ged.entity.VisibiliteDocument;

public record DocumentRequest(
        @NotBlank(message = "Le titre est obligatoire")
        String titre,

        String description,

        @NotNull(message = "Le type de document est obligatoire")
        TypeDocument typeDocument,

        VisibiliteDocument visibilite,
        UUID projetId,
        UUID employeId,
        Boolean accessibleCa
) {
}
