package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BudgetAnnuelRequest(
    @NotNull(message = "L'exercice (année) est obligatoire")
    Integer exercice,

    String intitule,

    @NotNull(message = "Le montant total est obligatoire")
    @Positive
    BigDecimal montantTotal,

    String devise,

    LocalDate dateVote
) {}
