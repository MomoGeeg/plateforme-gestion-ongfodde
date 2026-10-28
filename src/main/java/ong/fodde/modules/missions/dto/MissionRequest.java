package ong.fodde.modules.missions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MissionRequest(
    @NotNull UUID employeId,
    @NotNull UUID projetId,
    @NotBlank String destination,
    @NotNull LocalDate dateDebut,
    @NotNull LocalDate dateFin,
    @NotBlank String motif,
    @NotNull BigDecimal budgetEstime
) {}
