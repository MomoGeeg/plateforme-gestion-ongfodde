package ong.fodde.modules.acquisitions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.acquisitions.entity.CategorieAcquisition;
import ong.fodde.modules.acquisitions.entity.StatutAcquisition;

public record AcquisitionResponse(
    UUID id,
    String codeActif,
    String designation,
    String justification,
    CategorieAcquisition categorie,
    UUID projetId,
    String projetCode,
    BigDecimal valeur,
    LocalDate dateAcquisition,
    UUID soumisParId,
    String soumisPar,
    StatutAcquisition statut,
    String motifRejet,
    LocalDateTime createdAt
) {}
