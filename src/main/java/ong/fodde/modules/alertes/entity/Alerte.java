package ong.fodde.modules.alertes.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "alertes")
@Getter
@Setter
@NoArgsConstructor
public class Alerte extends BaseEntity {

    /** Clé de déduplication, ex: "PROJET_RETARD_<uuid>", "DEPENSE_ATTENTE_<uuid>" */
    @Column(name = "reference_key", length = 100, nullable = false, unique = true)
    private String referenceKey;

    @Column(name = "titre", length = 300, nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    /** CRITIQUE | URGENT | WARNING */
    @Column(name = "niveau", length = 20, nullable = false)
    private String niveau;

    /** PROJET | DEPENSE | RH | CONTRAT */
    @Column(name = "module", length = 30, nullable = false)
    private String module;

    /** Lien de navigation vers la ressource concernée */
    @Column(name = "lien", length = 300)
    private String lien;

    @Column(name = "lue", nullable = false)
    private boolean lue = false;
}
