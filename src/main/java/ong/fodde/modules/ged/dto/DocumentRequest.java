package ong.fodde.modules.ged.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.ged.entity.TypeDocument;

public record DocumentRequest(
        @NotBlank(message = "Le titre est obligatoire")
        String titre,

        String description,

        @NotNull(message = "Le type de document est obligatoire")
        TypeDocument typeDocument,

        UUID projetId,

        /** Employé concerné (bulletins salaire, contrats emploi, évaluations) */
        UUID concerneEmployeId,

        /**
         * Rôles en lecture — si null, les valeurs par défaut du type sont appliquées.
         * ROLE_SE et ROLE_ADMIN sont toujours ajoutés automatiquement.
         */
        Set<String> rolesLecture,

        /**
         * Rôles en écriture — si null, les valeurs par défaut du type sont appliquées.
         * ROLE_SE et ROLE_ADMIN sont toujours ajoutés automatiquement.
         */
        Set<String> rolesEcriture
) {
}
