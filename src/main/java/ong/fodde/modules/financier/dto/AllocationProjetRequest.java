package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AllocationProjetRequest(
    @NotNull(message = "L'identifiant du budget annuel est obligatoire")
    UUID budgetAnnuelId,

    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    @NotNull(message = "Le montant alloué est obligatoire")
    @Positive
    BigDecimal montantAlloue
) {}
