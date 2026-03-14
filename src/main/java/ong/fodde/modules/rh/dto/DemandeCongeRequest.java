package ong.fodde.modules.rh.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.rh.entity.TypeConge;

public record DemandeCongeRequest(
        @NotNull(message = "L'identifiant de l'employé est obligatoire")
        UUID employeId,

        @NotNull(message = "Le type de congé est obligatoire")
        TypeConge typeConge,

        @NotNull(message = "La date de début est obligatoire")
        LocalDate dateDebut,

        @NotNull(message = "La date de fin est obligatoire")
        LocalDate dateFin,

        String motif,

        String justificatifUrl
) {
}
