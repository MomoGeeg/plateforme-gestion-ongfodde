package ong.fodde.modules.projets.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RapportActiviteRequest(
    @NotNull(message = "L'identifiant de l'activité est obligatoire")
    UUID activiteId,

    @NotNull(message = "L'identifiant du projet est obligatoire")
    UUID projetId,

    @NotNull(message = "L'identifiant de l'auteur est obligatoire")
    UUID auteurId,

    @NotBlank(message = "Le titre du rapport est obligatoire")
    String titre,

    String descriptionAvancement,

    String problemesRencontres,

    String prochainesEtapes,

    String periode,

    String documentUrl
) {}
