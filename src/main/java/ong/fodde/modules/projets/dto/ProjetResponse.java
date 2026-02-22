package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.projets.entity.Devise;
import ong.fodde.modules.projets.entity.StatutProjet;

public record ProjetResponse(
    UUID id,
    String code,
    String titre,
    String description,
    String objectifGeneral,
    String zoneGeographique,
    LocalDate dateDebut,
    LocalDate dateFin,
    BigDecimal budgetTotal,
    Devise devise,
    StatutProjet statut,
    UUID responsableId,
    LocalDateTime createdAt
) {}
