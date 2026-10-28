package ong.fodde.modules.se.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.se.entity.NiveauIndicateur;
import ong.fodde.modules.se.entity.TypeIndicateur;

public record IndicateurResponse(
    UUID id,
    UUID projetId,
    String code,
    String libelle,
    TypeIndicateur type,
    NiveauIndicateur niveau,
    String unite,
    BigDecimal valeurBaseline,
    BigDecimal valeurCible,
    BigDecimal valeurAtteinte,
    String frequenceCollecte,
    String sourceVerification,
    UUID responsableId,
    LocalDateTime createdAt,
    Double tauxRealisation
) {}
