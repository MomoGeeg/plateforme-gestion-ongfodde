package ong.fodde.modules.alertes.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlerteResponse(
    UUID id,
    String referenceKey,
    String titre,
    String description,
    String niveau,
    String module,
    String lien,
    boolean lue,
    LocalDateTime createdAt
) {}
