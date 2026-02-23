package ong.fodde.modules.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.projets.entity.Devise;
import ong.fodde.modules.rh.entity.StatutContrat;
import ong.fodde.modules.rh.entity.TypeContrat;

public record ContratResponse(
        UUID id,
        UUID employeId,
        TypeContrat typeContrat,
        LocalDate dateDebut,
        LocalDate dateFin,
        BigDecimal salaireBase,
        Devise devise,
        String poste,
        StatutContrat statut,
        String documentUrl,
        LocalDateTime createdAt
) {
}
