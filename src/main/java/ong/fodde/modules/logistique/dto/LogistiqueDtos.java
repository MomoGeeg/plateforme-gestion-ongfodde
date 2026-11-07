package ong.fodde.modules.logistique.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import ong.fodde.modules.logistique.entity.CategorieMateriel;
import ong.fodde.modules.logistique.entity.EtatMateriel;
import ong.fodde.modules.logistique.entity.TypeCarburant;
import ong.fodde.modules.logistique.entity.TypeMouvement;

/** Requêtes et réponses de la logistique (matériel, mouvements, véhicules, carnet de bord). */
public final class LogistiqueDtos {

    private LogistiqueDtos() {
    }

    // ── Matériel ────────────────────────────────────────────────────────────

    /**
     * Création ou modification d'un bien. À la création seulement : état, projet,
     * responsable et localisation de départ, et l'acquisition d'origine. Ensuite,
     * ces informations changent par des mouvements.
     */
    public record MaterielRequest(
        String codeActif,
        @NotBlank(message = "La désignation est obligatoire") String designation,
        @NotNull(message = "La catégorie est obligatoire") CategorieMateriel categorie,
        String marqueModele,
        String numeroSerie,
        @PositiveOrZero(message = "La valeur ne peut pas être négative") BigDecimal valeurAcquisition,
        LocalDate dateAcquisition,
        String notes,
        EtatMateriel etat,
        String localisation,
        UUID projetId,
        UUID responsableId,
        UUID acquisitionId
    ) {}

    public record MaterielResponse(
        UUID id,
        String codeActif,
        String designation,
        CategorieMateriel categorie,
        String marqueModele,
        String numeroSerie,
        EtatMateriel etat,
        BigDecimal valeurAcquisition,
        LocalDate dateAcquisition,
        String localisation,
        UUID projetId,
        String projetCode,
        UUID responsableId,
        String responsableNom,
        String notes,
        UUID acquisitionId,
        LocalDateTime createdAt
    ) {}

    /**
     * Mouvement d'un bien. {@code responsableId} et {@code projetId} servent à
     * l'affectation ; {@code etatApres} au retour et à l'inventaire.
     */
    public record MouvementRequest(
        @NotNull(message = "Le type de mouvement est obligatoire") TypeMouvement type,
        LocalDate date,
        UUID responsableId,
        UUID projetId,
        String localisation,
        EtatMateriel etatApres,
        String notes
    ) {}

    public record MouvementResponse(
        UUID id,
        TypeMouvement type,
        LocalDate date,
        UUID responsableId,
        String responsableNom,
        UUID projetId,
        String projetCode,
        String etatAvant,
        String etatApres,
        String notes,
        String saisiPar,
        LocalDateTime createdAt
    ) {}

    // ── Véhicules ───────────────────────────────────────────────────────────

    public record VehiculeRequest(
        @NotBlank(message = "L'immatriculation est obligatoire") String immatriculation,
        @NotBlank(message = "La marque et le modèle sont obligatoires") String marqueModele,
        Integer annee,
        @PositiveOrZero(message = "Le kilométrage ne peut pas être négatif") Integer kilometrageActuel,
        @PositiveOrZero(message = "Le kilométrage de révision ne peut pas être négatif") Integer kilometrageRevision,
        EtatMateriel etat,
        UUID projetId,
        UUID conducteurHabituelId,
        LocalDate dateExpirationAssurance,
        LocalDate dateVisiteTechnique,
        TypeCarburant typeCarburant,
        String notes
    ) {}

    /** Alerte d'un véhicule : code (ASSURANCE_EXPIREE…) et libellé lisible. */
    public record AlerteVehicule(String code, String libelle) {}

    public record VehiculeResponse(
        UUID id,
        String immatriculation,
        String marqueModele,
        Integer annee,
        Integer kilometrageActuel,
        Integer kilometrageRevision,
        EtatMateriel etat,
        UUID projetId,
        String projetCode,
        UUID conducteurHabituelId,
        String conducteurHabituelNom,
        LocalDate dateExpirationAssurance,
        LocalDate dateVisiteTechnique,
        TypeCarburant typeCarburant,
        String notes,
        List<AlerteVehicule> alertes,
        /** Sortie en cours, ou vide si le véhicule est au garage. */
        UUID sortieEnCoursId,
        LocalDateTime createdAt
    ) {}

    // ── Carnet de bord ──────────────────────────────────────────────────────

    public record DepartRequest(
        @NotNull(message = "Choisissez le conducteur") UUID conducteurId,
        @NotBlank(message = "La destination est obligatoire") String destination,
        String motif,
        UUID projetId,
        /** Par défaut : kilométrage actuel du véhicule. */
        Integer kmDepart,
        /** Par défaut : maintenant. */
        LocalDateTime dateDepart
    ) {}

    public record RetourRequest(
        @NotNull(message = "Le kilométrage au retour est obligatoire") Integer kmRetour,
        /** Par défaut : maintenant. */
        LocalDateTime dateRetour,
        String etatRetour
    ) {}

    public record SortieResponse(
        UUID id,
        UUID vehiculeId,
        UUID conducteurId,
        String conducteurNom,
        UUID projetId,
        String projetCode,
        String destination,
        String motif,
        Integer kmDepart,
        Integer kmRetour,
        /** Distance parcourue, une fois revenu. */
        Integer distance,
        LocalDateTime dateDepart,
        LocalDateTime dateRetour,
        String etatRetour,
        String saisiPar
    ) {}

    // ── Personnes ───────────────────────────────────────────────────────────

    /** Compte actif, pour choisir un responsable ou un conducteur. */
    public record PersonneResponse(UUID id, String nom) {}
}
