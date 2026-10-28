package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ActiviteRequest(
    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    UUID parentId,

    @NotBlank(message = "Le titre de l'activité est obligatoire")
    String titre,

    String description,

    UUID responsableId,

    @NotNull(message = "La date prévue de début est obligatoire")
    LocalDate datePrevuDebut,

    @NotNull(message = "La date prévue de fin est obligatoire")
    LocalDate datePrevuFin,

    BigDecimal budgetAlloue
) {}
