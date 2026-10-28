package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.projets.entity.StatutDemandeActivite;

public record DemandeActiviteResponse(
        UUID id,
        UUID projetId,
        UUID demandeurId,
        String titre,
        String description,
        String justification,
        BigDecimal budgetEstime,
        LocalDate dateDebutSouhaitee,
        LocalDate dateFinSouhaitee,
        StatutDemandeActivite statut,
        UUID valideParCp,
        LocalDateTime dateValidationCp,
        UUID valideParRp,
        LocalDateTime dateValidationRp,
        UUID approuveParRaf,
        LocalDateTime dateApprobationRaf,
        String motifRejet,
        UUID activiteCreeeId,
        LocalDateTime createdAt
) {}
