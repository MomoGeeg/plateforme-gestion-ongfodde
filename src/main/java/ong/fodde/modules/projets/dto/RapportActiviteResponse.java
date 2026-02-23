package ong.fodde.modules.projets.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.projets.entity.StatutRapport;

public record RapportActiviteResponse(
        UUID id,
        UUID activiteId,
        UUID projetId,
        UUID auteurId,
        String titre,
        String descriptionAvancement,
        String problemesRencontres,
        String prochainesEtapes,
        String periode,
        StatutRapport statut,
        UUID valideParCp,
        LocalDateTime dateValidationCp,
        UUID valideParRp,
        LocalDateTime dateValidationRp,
        String motifRejet,
        String documentUrl,
        LocalDateTime createdAt
) {}
