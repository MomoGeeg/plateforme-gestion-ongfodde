package ong.fodde.modules.acquisitions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.acquisitions.entity.CategorieAcquisition;

public record AcquisitionRequest(
    @NotBlank String designation,
    String justification,
    CategorieAcquisition categorie,
    @NotNull UUID projetId,
    @NotNull BigDecimal valeur,
    @NotNull LocalDate dateAcquisition,
    String codeActif
) {}
