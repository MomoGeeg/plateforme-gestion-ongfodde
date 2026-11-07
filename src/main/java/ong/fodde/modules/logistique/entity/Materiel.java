package ong.fodde.modules.logistique.entity;

import java.math.BigDecimal;
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

/**
 * Bien de l'inventaire (table {@code materiels}). L'état, la personne et le projet
 * d'affectation changent par des mouvements, pour garder l'historique.
 */
@Entity
@Table(name = "materiels")
@Getter
@Setter
@NoArgsConstructor
public class Materiel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "code_actif", nullable = false, unique = true, length = 50)
    private String codeActif;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, columnDefinition = "categorie_mat")
    private CategorieMateriel categorie;

    @Column(name = "marque_modele")
    private String marqueModele;

    @Column(name = "numero_serie")
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, columnDefinition = "etat_materiel")
    private EtatMateriel etat = EtatMateriel.BON_ETAT;

    @Column(name = "valeur_acquisition")
    private BigDecimal valeurAcquisition;

    @Column(name = "date_acquisition")
    private LocalDate dateAcquisition;

    @Column(name = "localisation")
    private String localisation;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    /** Personne à qui le bien est affecté (compte utilisateur). */
    @Column(name = "responsable_id", columnDefinition = "uuid")
    private UUID responsableId;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** Acquisition validée d'où vient le bien (une seule fois). */
    @Column(name = "acquisition_id", columnDefinition = "uuid", unique = true)
    private UUID acquisitionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", columnDefinition = "uuid", updatable = false)
    private UUID createdBy;
}
