package ong.fodde.modules.logistique.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.logistique.entity.TypeMouvement;

public record MouvementResponse(
    UUID id,
    TypeMouvement type,
    UUID bienId,
    String bienDesignation,
    String bienCodeActif,
    UUID vehiculeId,
    String vehiculeImmatriculation,
    UUID projetId,
    UUID employeId,
    String localisationDepart,
    String localisationArrivee,
    LocalDate dateMouvement,
    String motif,
    String notes,
    LocalDateTime createdAt
) {}
