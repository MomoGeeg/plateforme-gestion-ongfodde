package ong.fodde.modules.ged.dto;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import ong.fodde.modules.ged.entity.StatutDocument;
import ong.fodde.modules.ged.entity.TypeDocument;

public record DocumentResponse(
        UUID id,
        String titre,
        String description,
        TypeDocument typeDocument,
        StatutDocument statut,
        boolean requiresValidation,
        Set<String> rolesLecture,
        Set<String> rolesEcriture,
        UUID projetId,
        UUID concerneEmployeId,
        Integer versionActuelle,
        String motifRejet,
        LocalDateTime dateValidation,
        UUID valideParId,
        UUID createdBy,
        String createdByNom,
        LocalDateTime createdAt
) {
}
