package ong.fodde.modules.logistique.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.logistique.entity.CategorieEquipement;
import ong.fodde.modules.logistique.entity.EtatBien;

public record BienRequest(
    @NotBlank String designation,
    @NotNull CategorieEquipement categorie,
    String marque,
    String modele,
    String numeroSerie,
    EtatBien etat,
    BigDecimal valeurAcquisition,
    LocalDate dateAcquisition,
    String localisationActuelle,
    UUID projetId,
    UUID responsableId,
    String notes
) {}
