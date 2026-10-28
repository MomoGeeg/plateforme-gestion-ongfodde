package ong.fodde.modules.alertes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlerteRequest(
    @NotBlank String titre,
    String description,
    @NotNull String niveau,   // CRITIQUE | URGENT | WARNING
    @NotBlank String module,
    String lien
) {}
