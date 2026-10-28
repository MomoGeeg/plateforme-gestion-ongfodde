package ong.fodde.modules.logistique.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.logistique.entity.EtatVehicule;
import ong.fodde.modules.logistique.entity.TypeCarburant;

public record VehiculeResponse(
    UUID id,
    String immatriculation,
    String marque,
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
    String notes,
    Long joursAvantAssurance,
    Long joursAvantVisite,
    boolean alerteAssurance,
    boolean alerteVisite,
    boolean alerteKilometrage,
    LocalDateTime createdAt
) {}
