package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.projets.entity.Devise;

public record ProjetRequest(
    @NotBlank(message = "Le code du projet est obligatoire")
    String code,

    @NotBlank(message = "Le titre du projet est obligatoire")
    String titre,

    String description,

    String objectifGeneral,

    String zoneGeographique,

    @NotNull(message = "La date de début est obligatoire")
    LocalDate dateDebut,

    @NotNull(message = "La date de fin est obligatoire")
    LocalDate dateFin,

    @NotNull(message = "Le budget total est obligatoire")
    BigDecimal budgetTotal,

    Devise devise,

    @NotNull(message = "Le responsable est obligatoire")
    UUID responsableId,

    // Membres de l'équipe projet — optionnel à la création, modifiable après
    List<UUID> equipeIds
) {}