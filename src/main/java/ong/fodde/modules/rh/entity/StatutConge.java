package ong.fodde.modules.rh.entity;

/**
 * Circuit : EN_ATTENTE (demande de l'employé), VALIDE_RH (validée par les RH), APPROUVE (par le RAF).
 * VALIDE_N1 : ancien accord du supérieur direct, plus utilisé.
 */
public enum StatutConge {
    EN_ATTENTE,
    VALIDE_N1,
    VALIDE_RH,
    APPROUVE,
    REJETE,
    ANNULE
}
