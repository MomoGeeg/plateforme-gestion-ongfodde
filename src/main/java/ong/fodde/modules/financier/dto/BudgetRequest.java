package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.projets.entity.Devise;

public record BudgetRequest(
    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    @NotNull(message = "Le montant total est obligatoire")
    BigDecimal montantTotal,

    @NotNull(message = "La devise est obligatoire")
    Devise devise
) {}
