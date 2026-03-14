package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LigneBudgetaireRequest(
    @NotNull(message = "L'identifiant de l'allocation projet est obligatoire")
    UUID allocationProjetId,

    @NotBlank(message = "Le code de la ligne budgétaire est obligatoire")
    String code,

    @NotBlank(message = "L'intitulé est obligatoire")
    String intitule,

    String categorie,

    @NotNull(message = "Le montant prévu est obligatoire")
    BigDecimal montantPrevu
) {}
