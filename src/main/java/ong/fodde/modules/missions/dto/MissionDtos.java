package ong.fodde.modules.missions.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ong.fodde.modules.missions.entity.CategorieFrais;
import ong.fodde.modules.missions.entity.StatutMission;

/** Requêtes et réponses du module Missions. */
public final class MissionDtos {

    private MissionDtos() {
    }

    /** Ordre de mission saisi par le demandeur. */
    public record MissionRequest(
            @NotNull(message = "Choisissez le projet qui finance la mission") UUID projetId,
            @NotBlank(message = "Indiquez la destination") String destination,
            @NotNull(message = "Indiquez la date de départ") LocalDate dateDebut,
            @NotNull(message = "Indiquez la date de retour") LocalDate dateFin,
            @NotBlank(message = "Indiquez l'objet de la mission") String motif,
            /** Fiches employés de l'équipe proposée. */
            List<UUID> participantIds,
            UUID chefMissionId,
            boolean besoinVehicule,
            String besoinsMateriel) {
    }

    /** Logistique : véhicule, chauffeur et matériel remis. */
    public record OrganisationRequest(UUID vehiculeId, UUID chauffeurId, String notesLogistique) {
    }

    public record FraisRequest(CategorieFrais categorie, String libelle, BigDecimal quantite, BigDecimal prixUnitaire) {
    }

    /** RAF : équipe définitive, budget et envoi éventuel au SE. */
    public record PreparationRequest(List<UUID> participantIds, UUID chefMissionId, List<FraisRequest> frais,
                                     boolean soumettreSe) {
    }

    public record ParticipantResponse(UUID employeId, String nom, String poste, String telephone, boolean chef) {
    }

    public record FraisResponse(CategorieFrais categorie, String libelle, BigDecimal quantite, BigDecimal prixUnitaire,
                                BigDecimal montant) {
    }

    public record MissionResponse(
            UUID id,
            UUID demandeurId,
            String demandeurNom,
            UUID n1Id,
            String n1Nom,
            UUID projetId,
            String projetCode,
            String destination,
            LocalDate dateDebut,
            LocalDate dateFin,
            long nbJours,
            String motif,
            boolean besoinVehicule,
            String besoinsMateriel,
            List<ParticipantResponse> participants,
            UUID vehiculeId,
            String vehiculeLibelle,
            UUID chauffeurId,
            String chauffeurNom,
            String chauffeurTelephone,
            String notesLogistique,
            List<FraisResponse> frais,
            BigDecimal budgetEstime,
            boolean soumettreSe,
            /** Vrai si le SE doit valider (montant au-delà du seuil ou case cochée). */
            boolean validationSeRequise,
            BigDecimal seuilSe,
            StatutMission statut,
            String motifRejet,
            UUID avanceId,
            LocalDateTime createdAt) {
    }

    public record PersonneDisponible(UUID id, String nom, String poste, String telephone) {
    }

    public record VehiculeDisponible(UUID id, String libelle) {
    }

    /** Ce qui est libre sur les dates de la mission. */
    public record Disponibilites(List<PersonneDisponible> agents, List<PersonneDisponible> chauffeurs,
                                 List<VehiculeDisponible> vehicules) {
    }
}
