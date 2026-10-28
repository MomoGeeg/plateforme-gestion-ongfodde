package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.projets.entity.StatutActivite;

public record ActiviteResponse(
        UUID id,
        UUID projetId,
        String titre,
        String description,
        UUID responsableId,
        UUID parentId,
        LocalDate datePrevuDebut,
        LocalDate datePrevuFin,
        LocalDate dateReelDebut,
        LocalDate dateReelFin,
        BigDecimal budgetAlloue,
        StatutActivite statut,
        Integer tauxAvancement,
        LocalDateTime createdAt
) {
}
