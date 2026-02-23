package ong.fodde.modules.rh.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.rh.entity.StatutEmploye;

public record EmployeResponse(
        UUID id,
        String matricule,
        UUID userId,
        String nom,
        String prenom,
        LocalDate dateNaissance,
        String lieuNaissance,
        String nationalite,
        String telephone,
        String adresse,
        String poste,
        String departement,
        LocalDate dateEmbauche,
        StatutEmploye statut,
        UUID superviseurId,
        LocalDateTime createdAt
) {
}
