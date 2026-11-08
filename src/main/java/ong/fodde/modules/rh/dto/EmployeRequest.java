package ong.fodde.modules.rh.dto;

import java.time.LocalDate;
import java.util.UUID;

import ong.fodde.modules.rh.entity.Pole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmployeRequest(
        UUID userId,

        @NotBlank(message = "Le matricule est obligatoire")
        String matricule,

        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        LocalDate dateNaissance,
        String lieuNaissance,
        String nationalite,
        String telephone,
        String adresse,

        @NotBlank(message = "Le poste est obligatoire")
        String poste,

        String departement,

        @NotNull(message = "La date d'embauche est obligatoire")
        LocalDate dateEmbauche,

        Pole pole,

        /** Supérieur direct (N+1) : identifiant d'une autre fiche employé. */
        UUID superieurId
) {
}
