package ong.fodde.modules.se.entity;

import java.time.LocalDate;
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

@Entity
@Table(name = "donnees_collectees")
@Getter
@Setter
@NoArgsConstructor
public class DonneeCollectee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "formulaire_id", nullable = false, columnDefinition = "uuid")
    private UUID formulaireId;

    @Column(name = "collecteur_id", nullable = false, columnDefinition = "uuid")
    private UUID collecteurId;

    @Column(name = "date_collecte", nullable = false)
    private LocalDate dateCollecte;

    @Column(name = "localisation", length = 200)
    private String localisation;

    @Column(name = "donnees", nullable = false, columnDefinition = "text")
    private String donnees;

    @Column(name = "valide", nullable = false)
    private Boolean valide = false;

    @Column(name = "valide_par", columnDefinition = "uuid")
    private UUID valideParId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
