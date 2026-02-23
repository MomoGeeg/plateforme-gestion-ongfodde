package ong.fodde.modules.ged.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.ged.entity.StatutDocument;
import ong.fodde.modules.ged.entity.TypeDocument;
import ong.fodde.modules.ged.entity.VisibiliteDocument;

public record DocumentResponse(
        UUID id,
        String titre,
        String description,
        TypeDocument typeDocument,
        StatutDocument statut,
        VisibiliteDocument visibilite,
        UUID projetId,
        UUID employeId,
        Integer versionActuelle,
        Boolean accessibleCa,
        LocalDateTime dateValidation,
        UUID valideParId,
        LocalDateTime createdAt
) {
}
