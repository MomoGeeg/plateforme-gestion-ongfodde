package ong.fodde.modules.se.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonneeCollecteeResponse(
    UUID id,
    UUID formulaireId,
    UUID collecteurId,
    LocalDate dateCollecte,
    String localisation,
    String donnees,
    Boolean valide,
    LocalDateTime createdAt
) {}
