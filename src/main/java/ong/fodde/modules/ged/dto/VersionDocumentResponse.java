package ong.fodde.modules.ged.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VersionDocumentResponse(
        UUID id,
        UUID documentId,
        Integer version,
        String url,
        Long taille,
        String typeFichier,
        UUID uploadePar,
        LocalDateTime uploadeLe
) {
}
