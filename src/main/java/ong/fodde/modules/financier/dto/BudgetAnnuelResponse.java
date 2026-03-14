package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.financier.entity.StatutBudgetAnnuel;

public record BudgetAnnuelResponse(
    UUID id,
    Integer exercice,
    String intitule,
    BigDecimal montantTotal,
    String devise,
    LocalDate dateVote,
    StatutBudgetAnnuel statut,
    String saisiPar,
    BigDecimal montantAlloue,      // somme des allocations existantes
    BigDecimal montantDisponible,  // montantTotal - montantAlloue
    int nombreProjets,
    LocalDateTime createdAt
) {}
