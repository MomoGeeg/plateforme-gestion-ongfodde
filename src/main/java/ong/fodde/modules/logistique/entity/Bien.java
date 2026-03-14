package ong.fodde.modules.logistique.entity;

import java.math.BigDecimal;
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
@Table(name = "biens")
@Getter
@Setter
@NoArgsConstructor
public class Bien extends BaseEntity {

    @Column(name = "code_actif", length = 30, unique = true)
    private String codeActif;

    @Column(name = "designation", length = 300, nullable = false)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, columnDefinition = "varchar(50)")
    private CategorieEquipement categorie;

    @Column(name = "marque", length = 100)
    private String marque;

    @Column(name = "modele", length = 100)
    private String modele;

    @Column(name = "numero_serie", length = 100)
    private String numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, columnDefinition = "varchar(50)")
    private EtatBien etat = EtatBien.BON_ETAT;

    @Column(name = "valeur_acquisition", precision = 18, scale = 2)
    private BigDecimal valeurAcquisition;

    @Column(name = "date_acquisition")
    private LocalDate dateAcquisition;

    @Column(name = "localisation_actuelle", length = 200)
    private String localisationActuelle;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "responsable_id", columnDefinition = "uuid")
    private UUID responsableId;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
