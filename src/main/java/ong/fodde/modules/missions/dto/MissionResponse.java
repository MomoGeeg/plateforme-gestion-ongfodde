package ong.fodde.modules.missions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.missions.entity.StatutMission;

public record MissionResponse(
    UUID id,
    UUID employeId,
    String employeNom,
    UUID projetId,
    String projetCode,
    String destination,
    LocalDate dateDebut,
    LocalDate dateFin,
    String motif,
    BigDecimal budgetEstime,
    StatutMission statut,
    String motifRejet,
    LocalDateTime createdAt
) {}
