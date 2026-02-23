package ong.fodde.modules.se.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.se.entity.StatutFormulaire;

public record FormulaireResponse(
    UUID id,
    UUID projetId,
    String titre,
    String description,
    StatutFormulaire statut,
    LocalDate dateDebut,
    LocalDate dateFin,
    LocalDateTime createdAt
) {}
