package ong.fodde.modules.se.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DonneeCollecteeRequest(
    @NotNull(message = "L'identifiant du formulaire est obligatoire")
    UUID formulaireId,

    @NotNull(message = "La date de collecte est obligatoire")
    LocalDate dateCollecte,

    String localisation,

    @NotBlank(message = "Les données sont obligatoires")
    String donnees
) {}
