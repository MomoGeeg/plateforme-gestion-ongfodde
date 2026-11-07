package ong.fodde.modules.financier.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PieceJustificativeResponse(
    UUID id,
    UUID depenseId,
    String nom,
    String typeFichier,
    Long taille,
    LocalDateTime createdAt
) {}
