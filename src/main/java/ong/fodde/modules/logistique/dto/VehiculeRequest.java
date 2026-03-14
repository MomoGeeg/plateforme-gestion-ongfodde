package ong.fodde.modules.logistique.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import ong.fodde.modules.logistique.entity.EtatVehicule;
import ong.fodde.modules.logistique.entity.TypeCarburant;

public record VehiculeRequest(
    @NotBlank String immatriculation,
    @NotBlank String marque,
    String modele,
    Integer annee,
    Integer kilometrageActuel,
    Integer kilometrageRevision,
    EtatVehicule etat,
    UUID projetId,
    UUID conducteurHabituelId,
    TypeCarburant typeCarburant,
    LocalDate dateExpirationAssurance,
    LocalDate dateVisiteTechnique,
    String notes
) {}
