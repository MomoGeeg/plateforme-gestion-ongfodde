package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.financier.entity.ModePaiement;
import ong.fodde.modules.projets.entity.Devise;

public record DepenseRequest(
    @NotNull(message = "La ligne budgétaire est obligatoire")
    UUID ligneBudgetaireId,

    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    @NotBlank(message = "Le libellé est obligatoire")
    String libelle,

    @NotNull(message = "Le montant est obligatoire")
    BigDecimal montant,

    @NotNull(message = "La devise est obligatoire")
    Devise devise,

    @NotNull(message = "Le mode de paiement est obligatoire")
    ModePaiement modePaiement,

    @NotBlank(message = "Le bénéficiaire est obligatoire")
    String beneficiaire,

    @NotNull(message = "La date de dépense est obligatoire")
    LocalDate dateDepense
) {}
