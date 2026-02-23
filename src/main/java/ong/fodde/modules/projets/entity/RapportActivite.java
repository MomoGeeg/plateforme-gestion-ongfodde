package ong.fodde.modules.projets.entity;

import java.time.LocalDateTime;
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
@Table(name = "rapports_activite")
@Getter
@Setter
@NoArgsConstructor
public class RapportActivite extends BaseEntity {

    @Column(name = "activite_id", nullable = false, columnDefinition = "uuid")
    private UUID activiteId;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "auteur_id", nullable = false, columnDefinition = "uuid")
    private UUID auteurId;

    @Column(name = "titre", length = 255, nullable = false)
    private String titre;

    @Column(name = "description_avancement", columnDefinition = "text")
    private String descriptionAvancement;

    @Column(name = "problemes_rencontres", columnDefinition = "text")
    private String problemesRencontres;

    @Column(name = "prochaines_etapes", columnDefinition = "text")
    private String prochainesEtapes;

    @Column(name = "periode", length = 100)
    private String periode;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", columnDefinition = "VARCHAR(50)")
    private StatutRapport statut;

    @Column(name = "valide_par_cp", columnDefinition = "uuid")
    private UUID valideParCp;

    @Column(name = "date_validation_cp")
    private LocalDateTime dateValidationCp;

    @Column(name = "valide_par_rp", columnDefinition = "uuid")
    private UUID valideParRp;

    @Column(name = "date_validation_rp")
    private LocalDateTime dateValidationRp;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;

    @Column(name = "document_url", columnDefinition = "text")
    private String documentUrl;
}
