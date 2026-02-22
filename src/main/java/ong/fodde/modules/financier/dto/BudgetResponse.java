package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ong.fodde.modules.financier.entity.StatutBudget;
import ong.fodde.modules.projets.entity.Devise;

public record BudgetResponse(
    UUID id,
    UUID projetId,
    Integer version,
    BigDecimal montantTotal,
    Devise devise,
    StatutBudget statut,
    LocalDate dateValidation,
    LocalDateTime createdAt,
    List<LigneBudgetaireResponse> lignes
) {}
