package ong.fodde.modules.auth.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Recense les données rattachées à un compte utilisateur (dépenses, projets,
 * missions, validations…), pour refuser sa suppression définitive tant qu'il en
 * a : l'historique perdrait son auteur, et une partie de ces liens n'est pas
 * protégée par la base (ils deviendraient orphelins sans erreur).
 *
 * Les liens purement techniques (rôles, jetons, notifications, lectures,
 * appartenance à une conversation) ne bloquent pas : ils sont supprimés avec
 * le compte.
 */
@Component
public class LiensUtilisateur {

    /** Table, colonnes qui désignent une personne, libellé au singulier et au pluriel. */
    private record Lien(String table, String[] colonnes, String singulier, String pluriel) {
        Lien(String table, String colonnes, String singulier, String pluriel) {
            this(table, colonnes.split(","), singulier, pluriel);
        }
    }

    private static final List<Lien> LIENS = List.of(
        new Lien("employes", "user_id", "fiche employé", "fiches employé"),
        new Lien("projets", "chef_projet_id,created_by,responsable_id", "projet", "projets"),
        new Lien("activites", "created_by,responsable_id", "activité", "activités"),
        new Lien("equipe_projet", "user_id", "équipe de projet", "équipes de projet"),
        new Lien("depenses", "saisie_par,valide_par,verifie_par", "dépense", "dépenses"),
        new Lien("avances", "beneficiaire_id,created_by", "avance", "avances"),
        new Lien("budgets_annuels", "saisi_par_id", "budget annuel", "budgets annuels"),
        new Lien("allocations_projets", "soumis_par_id,valide_par_id", "allocation budgétaire", "allocations budgétaires"),
        new Lien("financements", "created_by", "convention de financement", "conventions de financement"),
        new Lien("missions", "employe_id,n1_id,created_by,chef_mission_id,chauffeur_id", "mission", "missions"),
        new Lien("mission_participants", "employe_id", "participation à une mission", "participations à des missions"),
        new Lien("acquisitions", "soumis_par_id", "demande d'achat", "demandes d'achat"),
        new Lien("demandes_conge", "valide_par_raf", "congé approuvé", "congés approuvés"),
        new Lien("evaluations", "evaluateur_id", "évaluation", "évaluations"),
        new Lien("contrats", "created_by", "contrat saisi", "contrats saisis"),
        new Lien("employes", "created_by,superieur_id,superviseur_id", "fiche dont il est responsable",
            "fiches dont il est responsable"),
        new Lien("indicateurs", "created_by,responsable_id", "indicateur", "indicateurs"),
        new Lien("formulaires_collecte", "created_by", "formulaire de collecte", "formulaires de collecte"),
        new Lien("donnees_collectees", "collecte_par,valide_par", "donnée collectée", "données collectées"),
        new Lien("collectes", "collecteur_id,valide_par", "collecte", "collectes"),
        new Lien("documents", "valide_par_id,concerne_employe_id", "document", "documents"),
        new Lien("versions_document", "uploade_par", "version de document", "versions de document"),
        new Lien("historique_demandes", "auteur_id", "étape de validation", "étapes de validation"),
        new Lien("rapports", "genere_par_id", "rapport", "rapports"),
        new Lien("perimetres", "technicien_id,superviseur_id", "périmètre suivi", "périmètres suivis"),
        new Lien("mesures_perimetre", "saisi_par", "relevé d'indicateur", "relevés d'indicateurs"),
        new Lien("suivis_mensuels", "technicien_id,decide_par", "planning ou rapport mensuel", "plannings ou rapports mensuels"),
        new Lien("materiels", "created_by,responsable_id", "matériel", "matériels"),
        new Lien("mouvements_materiel", "created_by,responsable_id", "mouvement de matériel", "mouvements de matériel"),
        new Lien("vehicules", "conducteur_habituel_id,created_by", "véhicule", "véhicules"),
        new Lien("sorties_vehicule", "conducteur_id,created_by", "sortie de véhicule", "sorties de véhicule"),
        new Lien("messages", "auteur_id", "message", "messages"),
        new Lien("annonces", "auteur_id", "annonce", "annonces"),
        new Lien("conversations", "cree_par", "conversation", "conversations"),
        new Lien("evenements_calendrier", "cree_par", "événement", "événements"),
        new Lien("audit_logs", "user_id", "entrée du journal d'activité", "entrées du journal d'activité"));

    private final JdbcTemplate jdbc;

    public LiensUtilisateur(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Données rattachées au compte, sous forme lisible (« 2 dépenses », « 1 projet »…).
     * Liste vide : le compte peut être supprimé sans rien casser.
     */
    public List<String> lister(UUID userId) {
        // Une seule requête : un compteur par lien. Les noms de tables et de colonnes viennent de la
        // liste ci-dessus (constantes), l'identifiant passe en paramètre.
        StringBuilder sql = new StringBuilder();
        List<Object> parametres = new ArrayList<>();
        for (int i = 0; i < LIENS.size(); i++) {
            Lien l = LIENS.get(i);
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            sql.append("SELECT ").append(i).append(" AS rang, COUNT(*) AS n FROM ").append(l.table()).append(" WHERE ");
            for (int c = 0; c < l.colonnes().length; c++) {
                if (c > 0) {
                    sql.append(" OR ");
                }
                sql.append(l.colonnes()[c]).append(" = CAST(? AS uuid)");
                parametres.add(userId.toString());
            }
        }
        List<String> resultat = new ArrayList<>();
        jdbc.query(sql.toString(), rs -> {
            long n = rs.getLong("n");
            if (n > 0) {
                Lien l = LIENS.get(rs.getInt("rang"));
                resultat.add(n + " " + (n > 1 ? l.pluriel() : l.singulier()));
            }
        }, parametres.toArray());
        return resultat;
    }
}
