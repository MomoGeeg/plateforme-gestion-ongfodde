package ong.fodde.modules.rapports.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.rapports.entity.TypeRapport;

public record RapportResponse(
    UUID id,
    TypeRapport type,
    String typeLabel,
    String periode,
    String projetCode,
    String bailleur,
    String format,
    String taille,
    Boolean archive,
    String generePar,
    LocalDateTime dateGeneration
) {}
