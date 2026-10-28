package ong.fodde.modules.ged.dto;

import jakarta.validation.constraints.NotBlank;

public record VersionDocumentRequest(
        @NotBlank(message = "L'URL du fichier est obligatoire")
        String url,

        Long taille,
        String typeFichier
) {
}
