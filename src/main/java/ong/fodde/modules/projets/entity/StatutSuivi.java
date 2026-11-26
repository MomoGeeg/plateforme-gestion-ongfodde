package ong.fodde.modules.projets.entity;

/**
 * SOUMIS : chez le superviseur (ou le coordonnateur s'il n'y a pas de superviseur) ;
 * TRANSMIS : visé par le superviseur, chez le coordonnateur du projet ;
 * VALIDE : validé par le coordonnateur, visible du RAF, du RP, du SE et du suivi-évaluation ;
 * A_REVOIR : renvoyé à son auteur avec un commentaire.
 */
public enum StatutSuivi {
    SOUMIS,
    TRANSMIS,
    VALIDE,
    A_REVOIR
}
