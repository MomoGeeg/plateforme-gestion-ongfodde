package ong.fodde.modules.logistique.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.logistique.entity.CategorieEquipement;
import ong.fodde.modules.logistique.entity.EtatBien;

public record BienResponse(
    UUID id,
    String codeActif,
    String designation,
    CategorieEquipement categorie,
    String marque,
    String modele,
    String numeroSerie,
    EtatBien etat,
    BigDecimal valeurAcquisition,
    LocalDate dateAcquisition,
    String localisationActuelle,
    UUID projetId,
    UUID responsableId,
    String notes,
    LocalDateTime createdAt
) {}
