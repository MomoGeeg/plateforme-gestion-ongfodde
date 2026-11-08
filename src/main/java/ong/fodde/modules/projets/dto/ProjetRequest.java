package ong.fodde.modules.projets.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.projets.entity.Devise;

/**
 * Fiche d'un projet. Le budget total n'est plus saisi : c'est la somme du budget prévisionnel.
 */
public record ProjetRequest(
    /** Facultatif : attribué automatiquement (PROJ-AAAA-NNN) s'il est vide. */
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

    Devise devise,

    /** Responsable du projet (compte) ; par défaut, la personne qui crée le projet. */
    UUID responsableId,

    /** Chef de projet (compte, en général un chargé de programme). */
    UUID chefProjetId,

    /** Membres de l'équipe (comptes) — facultatif à la création. */
    List<UUID> equipeIds
) {}
