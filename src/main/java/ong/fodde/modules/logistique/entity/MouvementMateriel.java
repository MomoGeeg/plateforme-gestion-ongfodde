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

/** Mouvement d'un bien (table {@code mouvements_materiel}) : jamais modifié après coup. */
@Entity
@Table(name = "mouvements_materiel")
@Getter
@Setter
@NoArgsConstructor
public class MouvementMateriel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "materiel_id", nullable = false, columnDefinition = "uuid", updatable = false)
    private UUID materielId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mvt", nullable = false, columnDefinition = "type_mouvement", updatable = false)
    private TypeMouvement type;

    @Column(name = "projet_id", columnDefinition = "uuid", updatable = false)
    private UUID projetId;

    /** Personne à qui le bien est affecté (vide pour un retour, une réparation…). */
    @Column(name = "responsable_id", columnDefinition = "uuid", updatable = false)
    private UUID responsableId;

    @Column(name = "date_mvt", nullable = false, updatable = false)
    private LocalDate dateMouvement;

    @Column(name = "etat_avant", updatable = false)
    private String etatAvant;

    @Column(name = "etat_apres", updatable = false)
    private String etatApres;

    @Column(name = "notes", columnDefinition = "text", updatable = false)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", columnDefinition = "uuid", updatable = false)
    private UUID createdBy;
}
