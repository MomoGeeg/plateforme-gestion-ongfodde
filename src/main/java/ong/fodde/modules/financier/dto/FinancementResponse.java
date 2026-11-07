package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.projets.entity.Devise;

public record FinancementResponse(
    UUID id,
    UUID bailleurId,
    String bailleurNom,
    String bailleurSigle,
    UUID projetId,
    String projetCode,
    String projetTitre,
    String referenceConvention,
    BigDecimal montantAccorde,
    Devise devise,
    LocalDate dateDebut,
    LocalDate dateFin,
    String conditionsReporting,
    LocalDateTime createdAt
) {}
