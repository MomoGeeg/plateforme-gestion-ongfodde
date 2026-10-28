package ong.fodde.modules.se.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FormulaireRequest(
    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    @NotBlank(message = "Le titre du formulaire est obligatoire")
    String titre,

    String description,

    LocalDate dateDebut,

    LocalDate dateFin
) {}
