package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.projets.entity.TypeActivite;

public record ActiviteRequest(
    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    UUID parentId,

    /** Résultat attendu auquel l'activité contribue (cadre logique). */
    UUID resultatId,

    @NotBlank(message = "Le titre de l'activité est obligatoire")
    String titre,

    String description,

    UUID responsableId,

    @NotNull(message = "La date prévue de début est obligatoire")
    LocalDate datePrevuDebut,

    @NotNull(message = "La date prévue de fin est obligatoire")
    LocalDate datePrevuFin,

    BigDecimal budgetAlloue,

    /** ACTIVITE par défaut ; FORMATION, SENSIBILISATION ou SUIVI sur le terrain. */
    TypeActivite type,

    UUID perimetreId,

    /** Village, quartier, école… Les participants viennent de la liste de présence, pas d'ici. */
    String lieu
) {}
