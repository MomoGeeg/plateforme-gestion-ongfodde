package ong.fodde.modules.missions.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
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

@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    /** Compte de la personne qui a demandé la mission (colonne historique « employe_id »). */
    @Column(name = "employe_id", nullable = false, columnDefinition = "uuid")
    private UUID employeId;

    /** Compte du supérieur direct qui doit donner son accord ; vide si l'étape est sautée. */
    @Column(name = "n1_id", columnDefinition = "uuid")
    private UUID n1Id;

    /** Équipe : fiches employés (un chauffeur ou un agent n'a pas forcément de compte). */
    @ElementCollection
    @CollectionTable(name = "mission_participants", joinColumns = @JoinColumn(name = "mission_id"))
    @Column(name = "employe_id", columnDefinition = "uuid")
    private Set<UUID> participantIds = new LinkedHashSet<>();

    @Column(name = "chef_mission_id", columnDefinition = "uuid")
    private UUID chefMissionId;

    @Column(name = "besoin_vehicule", nullable = false)
    private boolean besoinVehicule;

    @Column(name = "besoins_materiel", columnDefinition = "text")
    private String besoinsMateriel;

    @Column(name = "vehicule_id", columnDefinition = "uuid")
    private UUID vehiculeId;

    /** Fiche employé du chauffeur. */
    @Column(name = "chauffeur_id", columnDefinition = "uuid")
    private UUID chauffeurId;

    @Column(name = "notes_logistique", columnDefinition = "text")
    private String notesLogistique;

    @ElementCollection
    @CollectionTable(name = "mission_frais", joinColumns = @JoinColumn(name = "mission_id"))
    @OrderColumn(name = "ordre")
    private List<FraisMission> frais = new ArrayList<>();

    /** Le RAF peut soumettre au SE une mission sous le seuil. */
    @Column(name = "soumettre_se", nullable = false)
    private boolean soumettreSe;

    /** Avance créée par la comptabilité au déblocage des fonds. */
    @Column(name = "avance_id", columnDefinition = "uuid")
    private UUID avanceId;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "destination", length = 300, nullable = false)
    private String destination;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "motif", columnDefinition = "text", nullable = false)
    private String motif;

    @Column(name = "budget_estime", nullable = false)
    private BigDecimal budgetEstime = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_mission")
    private StatutMission statut = StatutMission.SOUMISE;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

