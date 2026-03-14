package ong.fodde.modules.ged.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public class Document extends BaseEntity {

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, columnDefinition = "type_document")
    private TypeDocument typeDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_document")
    private StatutDocument statut = StatutDocument.BROUILLON;

    /** Rôles autorisés en lecture seule */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_roles_lecture",
            joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "role")
    private Set<String> rolesLecture = new HashSet<>();

    /** Rôles autorisés en écriture (implique la lecture) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "document_roles_ecriture",
            joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "role")
    private Set<String> rolesEcriture = new HashSet<>();

    /** Employé personnellement concerné (bulletins salaire, contrats emploi, évaluations) */
    @Column(name = "concerne_employe_id", columnDefinition = "uuid")
    private UUID concerneEmployeId;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "version_actuelle")
    private Integer versionActuelle = 1;

    /** true pour types nécessitant validation SE (CONVENTION_BAILLEUR, RAPPORT_*, etc.) */
    @Column(name = "requires_validation", nullable = false)
    private boolean requiresValidation = false;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "valide_par_id", columnDefinition = "uuid")
    private UUID valideParId;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;
}
