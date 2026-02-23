package ong.fodde.modules.ged.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VersionDocumentRequest(
        @NotNull(message = "L'identifiant du document est obligatoire")
        UUID documentId,

        @NotBlank(message = "L'URL du fichier est obligatoire")
        String url,

        Long taille,
        String typeFichier,

        @NotNull(message = "L'identifiant de l'uploader est obligatoire")
        UUID uploadePar
) {
}
