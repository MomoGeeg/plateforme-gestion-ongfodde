package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ong.fodde.modules.financier.entity.StatutAllocation;

public record AllocationProjetResponse(
    UUID id,
    UUID budgetAnnuelId,
    Integer exercice,
    UUID projetId,
    String projetCode,
    String projetTitre,
    String bailleur,
    BigDecimal montantAlloue,
    StatutAllocation statut,
    String motifRejet,
    String soumisParNom,
    String valideParNom,
    LocalDate dateValidation,
    List<LigneBudgetaireResponse> lignes,
    BigDecimal montantPrevu,    // somme lignes.montantPrevu
    BigDecimal montantDepense,  // somme lignes.montantDepense
    BigDecimal soldeDisponible, // montantAlloue - montantDepense
    LocalDateTime createdAt
) {}
