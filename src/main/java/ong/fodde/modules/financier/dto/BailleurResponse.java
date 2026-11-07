package ong.fodde.modules.financier.dto;

import java.util.UUID;

public record BailleurResponse(
    UUID id,
    String nom,
    String sigle,
    String pays,
    String contact,
    String email
) {}
