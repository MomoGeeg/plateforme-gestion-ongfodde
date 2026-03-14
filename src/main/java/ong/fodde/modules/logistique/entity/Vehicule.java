package ong.fodde.modules.logistique.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "vehicules")
@Getter
@Setter
@NoArgsConstructor
public class Vehicule extends BaseEntity {

    @Column(name = "immatriculation", length = 20, nullable = false, unique = true)
    private String immatriculation;

    @Column(name = "marque", length = 100, nullable = false)
    private String marque;

    @Column(name = "modele", length = 100)
    private String modele;

    @Column(name = "annee")
    private Integer annee;

    @Column(name = "kilometrage_actuel")
    private Integer kilometrageActuel = 0;

    @Column(name = "kilometrage_revision")
    private Integer kilometrageRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, columnDefinition = "varchar(50)")
    private EtatVehicule etat = EtatVehicule.BON;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "conducteur_habituel_id", columnDefinition = "uuid")
    private UUID conducteurHabituelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_carburant", columnDefinition = "varchar(50)")
    private TypeCarburant typeCarburant = TypeCarburant.DIESEL;

    @Column(name = "date_expiration_assurance")
    private LocalDate dateExpirationAssurance;

    @Column(name = "date_visite_technique")
    private LocalDate dateVisiteTechnique;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
