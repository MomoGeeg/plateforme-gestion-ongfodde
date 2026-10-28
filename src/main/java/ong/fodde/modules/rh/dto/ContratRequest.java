package ong.fodde.modules.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.projets.entity.Devise;
import ong.fodde.modules.rh.entity.TypeContrat;

public record ContratRequest(
        @NotNull(message = "L'identifiant de l'employé est obligatoire")
        UUID employeId,

        @NotNull(message = "Le type de contrat est obligatoire")
        TypeContrat typeContrat,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        LocalDate dateFin,

        @NotNull(message = "Le salaire de base est obligatoire")
        BigDecimal salaireBase,

        Devise devise,
        String poste,
        String documentUrl
) {
}
