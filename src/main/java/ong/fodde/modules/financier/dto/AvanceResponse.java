package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.financier.entity.StatutAvance;

public record AvanceResponse(
        UUID id,
        UUID beneficiaireId,
        String beneficiaireNom,
        UUID projetId,
        String projetCode,
        BigDecimal montant,
        String motif,
        StatutAvance statut,
        LocalDate dateOctroi,
        LocalDate dateJustification,
        BigDecimal montantJustifie,
        long joursEcoules,
        LocalDateTime createdAt
) {
}
