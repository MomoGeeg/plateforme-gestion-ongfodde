package ong.fodde.modules.rh.entity;

import java.time.LocalDate;
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
@Table(name = "demandes_conge")
@Getter
@Setter
@NoArgsConstructor
public class DemandeConge extends BaseEntity {

    @Column(name = "employe_id", nullable = false, columnDefinition = "uuid")
    private UUID employeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_conge", nullable = false, columnDefinition = "type_conge")
    private TypeConge typeConge;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "motif", columnDefinition = "text")
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_conge")
    private StatutConge statut = StatutConge.EN_ATTENTE;

    @Column(name = "valide_par_n1", columnDefinition = "uuid")
    private UUID valideParN1;

    @Column(name = "valide_par_rh", columnDefinition = "uuid")
    private UUID valideParRh;

    @Column(name = "date_validation_n1")
    private LocalDateTime dateValidationN1;

    @Column(name = "date_validation_rh")
    private LocalDateTime dateValidationRh;

    @Column(name = "justificatif_url")
    private String justificatifUrl;

    @Column(name = "motif_rejet")
    private String motifRejet;

    @Column(name = "nb_jours")
    private Integer nbJours;
}
