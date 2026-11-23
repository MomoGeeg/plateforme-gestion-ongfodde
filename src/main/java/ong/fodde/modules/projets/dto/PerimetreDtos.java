package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class PerimetreDtos {

    private PerimetreDtos() {
    }

    public record PerimetreRequest(
            @NotNull(message = "Le projet est obligatoire") UUID projetId,
            @NotBlank(message = "Le nom du périmètre est obligatoire") String nom,
            @NotBlank(message = "Le village est obligatoire") String village,
            String commune,
            String departement,
            BigDecimal superficieHa,
            UUID technicienId,
            UUID superviseurId,
            BigDecimal latitude,
            BigDecimal longitude,
            List<String> cultures) {
    }

    public record PerimetreResponse(
            UUID id,
            UUID projetId,
            String projetCode,
            String projetTitre,
            String nom,
            String village,
            String commune,
            String departement,
            BigDecimal superficieHa,
            UUID technicienId,
            String technicienNom,
            UUID superviseurId,
            String superviseurNom,
            long nbActivites,
            BigDecimal latitude,
            BigDecimal longitude,
            List<String> cultures) {
    }
}
