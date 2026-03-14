package ong.fodde.modules.logistique.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.logistique.entity.TypeMouvement;

public record MouvementRequest(
    @NotNull TypeMouvement type,
    UUID bienId,
    UUID vehiculeId,
    UUID projetId,
    UUID employeId,
    String localisationDepart,
    String localisationArrivee,
    @NotNull LocalDate dateMouvement,
    String motif,
    String notes
) {}
