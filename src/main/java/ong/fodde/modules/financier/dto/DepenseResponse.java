package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.financier.entity.ModePaiement;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.projets.entity.Devise;

public record DepenseResponse(
    UUID id,
    String numeroPiece,
    UUID ligneBudgetaireId,
    UUID projetId,
    String libelle,
    BigDecimal montant,
    Devise devise,
    ModePaiement modePaiement,
    String beneficiaire,
    LocalDate dateDepense,
    StatutDepense statut,
    UUID saisiParId,
    LocalDateTime createdAt
) {}
