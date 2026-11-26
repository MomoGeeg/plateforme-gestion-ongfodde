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

/**
 * Planning ou rapport du mois d'un technicien ou d'un animateur. Un seul par type, par mois et par projet.
 * Circuit : superviseur (visa, correction du texte au besoin), puis coordonnateur du projet.
 */
@Entity
@Table(name = "suivis_mensuels")
@Getter
@Setter
@NoArgsConstructor
public class SuiviMensuel extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type_suivi", nullable = false, length = 20)
    private TypeSuivi type;

    @Column(name = "technicien_id", nullable = false, columnDefinition = "uuid")
    private UUID technicienId;

    /** Mois concerné, au format AAAA-MM. */
    @Column(name = "mois", nullable = false, length = 7)
    private String mois;

    /** Projet concerné : le coordonnateur de ce projet valide le document. */
    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    /** Planning : activités prévues ; rapport : résumé facultatif (le rapport est le fichier Word). */
    @Column(name = "contenu", columnDefinition = "text")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutSuivi statut = StatutSuivi.SOUMIS;

    @Column(name = "commentaire", columnDefinition = "text")
    private String commentaire;

    @Column(name = "decide_par", columnDefinition = "uuid")
    private UUID decidePar;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;

    /** Superviseur qui a visé le document et l'a transmis au coordonnateur. */
    @Column(name = "vise_par", columnDefinition = "uuid")
    private UUID visePar;

    @Column(name = "date_visa")
    private LocalDateTime dateVisa;

    /** Texte remis par l'auteur, conservé quand le superviseur l'a corrigé. */
    @Column(name = "contenu_initial", columnDefinition = "text")
    private String contenuInitial;

    /** Fichier Word du rapport (version en cours, corrigée le cas échéant par le superviseur). */
    @Column(name = "fichier_nom", length = 255)
    private String fichierNom;

    @Column(name = "fichier_chemin", length = 500)
    private String fichierChemin;

    @Column(name = "fichier_taille")
    private Long fichierTaille;

    /** Fichier remis par l'auteur, gardé quand le superviseur dépose une version corrigée. */
    @Column(name = "fichier_initial_nom", length = 255)
    private String fichierInitialNom;

    @Column(name = "fichier_initial_chemin", length = 500)
    private String fichierInitialChemin;
}
