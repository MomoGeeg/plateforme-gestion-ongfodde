package ong.fodde.modules.financier.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LigneBudgetaireResponse(
    UUID id,
    UUID budgetId,
    String code,
    String intitule,
    String categorie,
    BigDecimal montantPrevu,
    BigDecimal montantEngage,
    BigDecimal montantDepense,
    Boolean estGelee,
    BigDecimal soldeDisponible,
    Double tauxConsommation
) {}
