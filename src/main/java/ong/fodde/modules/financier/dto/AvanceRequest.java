package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AvanceRequest(
        @NotNull(message = "L'identifiant du bénéficiaire est obligatoire")
        UUID beneficiaireId,

        @NotNull(message = "L'identifiant du projet est obligatoire")
        UUID projetId,

        @NotNull(message = "Le montant est obligatoire")
        BigDecimal montant,

        String motif,
        LocalDate dateOctroi
) {
}
