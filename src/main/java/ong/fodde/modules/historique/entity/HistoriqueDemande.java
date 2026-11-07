package ong.fodde.modules.historique.entity;

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

/**
 * Une étape du circuit d'une demande : passage d'un statut à un autre,
 * par qui, quand, avec quel motif. Une ligne n'est jamais modifiée.
 */
@Entity
@Table(name = "historique_demandes")
@Getter
@Setter
@NoArgsConstructor
public class HistoriqueDemande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_demande", nullable = false, length = 20, updatable = false)
    private TypeDemande typeDemande;

    @Column(name = "demande_id", nullable = false, columnDefinition = "uuid", updatable = false)
    private UUID demandeId;

    /** Vide pour la création de la demande. */
    @Column(name = "statut_avant", length = 30, updatable = false)
    private String statutAvant;

    @Column(name = "statut_apres", nullable = false, length = 30, updatable = false)
    private String statutApres;

    @Column(name = "auteur_id", columnDefinition = "uuid", updatable = false)
    private UUID auteurId;

    /** Nom de l'auteur au moment de l'action (reste lisible si le compte change ou disparaît). */
    @Column(name = "auteur_nom", length = 200, updatable = false)
    private String auteurNom;

    @Column(name = "motif", columnDefinition = "text", updatable = false)
    private String motif;

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction;
}
