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
@Table(name = "mouvements_materiel")
@Getter
@Setter
@NoArgsConstructor
public class MouvementMateriel extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, columnDefinition = "varchar(50)")
    private TypeMouvement type;

    @Column(name = "bien_id", columnDefinition = "uuid")
    private UUID bienId;

    @Column(name = "vehicule_id", columnDefinition = "uuid")
    private UUID vehiculeId;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "employe_id", columnDefinition = "uuid")
    private UUID employeId;

    @Column(name = "localisation_depart", length = 200)
    private String localisationDepart;

    @Column(name = "localisation_arrivee", length = 200)
    private String localisationArrivee;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDate dateMouvement;

    @Column(name = "motif", columnDefinition = "text")
    private String motif;

    @Column(name = "valide_par_id", columnDefinition = "uuid")
    private UUID valideParId;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
