package ong.fodde.modules.logistique.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ligne du carnet de bord (table {@code sorties_vehicule}) : départ, puis retour
 * qui met à jour le kilométrage du véhicule. Une seule sortie en cours par véhicule.
 */
@Entity
@Table(name = "sorties_vehicule")
@Getter
@Setter
@NoArgsConstructor
public class SortieVehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "vehicule_id", nullable = false, columnDefinition = "uuid", updatable = false)
    private UUID vehiculeId;

    @Column(name = "conducteur_id", nullable = false, columnDefinition = "uuid")
    private UUID conducteurId;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "destination")
    private String destination;

    @Column(name = "motif", columnDefinition = "text")
    private String motif;

    @Column(name = "km_depart", nullable = false)
    private Integer kmDepart;

    @Column(name = "km_retour")
    private Integer kmRetour;

    @Column(name = "date_depart", nullable = false)
    private LocalDateTime dateDepart;

    @Column(name = "date_retour")
    private LocalDateTime dateRetour;

    /** Remarques au retour (dégâts, plein à faire…). */
    @Column(name = "etat_retour")
    private String etatRetour;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", columnDefinition = "uuid", updatable = false)
    private UUID createdBy;
}
