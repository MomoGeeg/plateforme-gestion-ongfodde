package ong.fodde.modules.rapports.dto;

import jakarta.validation.constraints.NotBlank;
import ong.fodde.modules.rapports.entity.TypeRapport;

public record RapportRequest(
    TypeRapport type,
    @NotBlank String typeLabel,
    String periode,
    String projetCode,
    String bailleur,
    String format
) {}
