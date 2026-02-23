package ong.fodde.modules.rh.entity;

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

    @Column(name = "approbateur_id", columnDefinition = "uuid")
    private UUID approbateurId;

    @Column(name = "commentaire_approbateur", columnDefinition = "text")
    private String commentaireApprobateur;
}
