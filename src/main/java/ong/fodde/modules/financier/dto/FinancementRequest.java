package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ong.fodde.modules.projets.entity.Devise;

public record FinancementRequest(
    @NotNull(message = "Le bailleur est obligatoire")
    UUID bailleurId,
    @NotNull(message = "Le projet est obligatoire")
    UUID projetId,
    @NotBlank(message = "La référence de la convention est obligatoire")
    @Size(max = 100)
    String referenceConvention,
    @NotNull(message = "Le montant accordé est obligatoire")
    @Positive(message = "Le montant accordé doit être positif")
    BigDecimal montantAccorde,
    @NotNull(message = "La devise est obligatoire")
    Devise devise,
    LocalDate dateDebut,
    LocalDate dateFin,
    String conditionsReporting
) {}
