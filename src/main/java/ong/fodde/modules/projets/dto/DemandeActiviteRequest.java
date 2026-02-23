package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DemandeActiviteRequest(
    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    @NotNull(message = "L'identifiant du demandeur est obligatoire")
    UUID demandeurId,

    @NotBlank(message = "Le titre de la demande est obligatoire")
    String titre,

    String description,

    String justification,

    BigDecimal budgetEstime,

    LocalDate dateDebutSouhaitee,

    LocalDate dateFinSouhaitee
) {}
