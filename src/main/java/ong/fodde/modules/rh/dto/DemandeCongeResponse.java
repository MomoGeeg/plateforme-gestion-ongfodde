package ong.fodde.modules.rh.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.rh.entity.StatutConge;
import ong.fodde.modules.rh.entity.TypeConge;

public record DemandeCongeResponse(
        UUID id,
        UUID employeId,
        TypeConge typeConge,
        LocalDate dateDebut,
        LocalDate dateFin,
        String motif,
        StatutConge statut,
        UUID approbateurId,
        String commentaireApprobateur,
        LocalDateTime createdAt,
        Long nombreJours
) {
}
