package ong.fodde.modules.ged.entity;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "visibilite", nullable = false, columnDefinition = "visibilite_doc")
    private VisibiliteDocument visibilite = VisibiliteDocument.PRIVE;

    @Column(name = "projet_id", columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "employe_id", columnDefinition = "uuid")
    private UUID employeId;

    @Column(name = "version_actuelle")
    private Integer versionActuelle = 1;

    @Column(name = "accessible_ca")
    private Boolean accessibleCa = false;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "valide_par", columnDefinition = "uuid")
    private UUID valideParId;
}
