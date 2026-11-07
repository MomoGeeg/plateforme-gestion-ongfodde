package ong.fodde.modules.logistique.entity;

/** Type de mouvement d'un bien (type PostgreSQL {@code type_mouvement}). */
public enum TypeMouvement {
    /** Remis à une personne ou à un projet. */
    AFFECTATION,
    /** Rendu au magasin, ou revenu de réparation. */
    RETOUR,
    /** Parti en réparation. */
    REPARATION,
    /** Sorti définitivement de l'inventaire actif (panne, perte, vol…). */
    MISE_HORS_SERVICE,
    /** Constat lors d'un inventaire, ou entrée dans l'inventaire. */
    INVENTAIRE
}
