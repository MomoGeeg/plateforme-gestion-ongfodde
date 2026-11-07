package ong.fodde.modules.logistique.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Véhicule du parc (table {@code vehicules}). Le kilométrage avance avec le carnet de bord. */
@Entity
@Table(name = "vehicules")
@Getter
@Setter
@NoArgsConstructor
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "immatriculation", nullable = false, unique = true)
    private String immatriculation;

    @Column(name = "marque_modele", nullable = false)
    private String marqueModele;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "kilometrage_actuel", nullable = false)
    private Integer kilometrageActuel = 0;

    /** Kilométrage auquel la prochaine révision est due. */
    @Column(name = "kilometrage_revision")
    private Integer kilometrageRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, columnDefinition = "etat_materiel")
    private EtatMateriel etat = EtatMateriel.BON_ETAT;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "conducteur_habituel_id", columnDefinition = "uuid")
    private UUID conducteurHabituelId;

    @Column(name = "date_exp_assurance")
    private LocalDate dateExpirationAssurance;

    @Column(name = "date_visite_technique")
    private LocalDate dateVisiteTechnique;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_carburant", length = 20)
    private TypeCarburant typeCarburant;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", columnDefinition = "uuid", updatable = false)
    private UUID createdBy;
}
