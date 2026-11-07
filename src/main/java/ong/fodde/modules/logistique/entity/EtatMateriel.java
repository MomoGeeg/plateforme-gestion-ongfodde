package ong.fodde.modules.logistique.entity;

/** État d'un bien ou d'un véhicule (type PostgreSQL {@code etat_materiel}). */
public enum EtatMateriel {
    NEUF,
    BON_ETAT,
    USE,
    EN_REPARATION,
    HORS_SERVICE
}
