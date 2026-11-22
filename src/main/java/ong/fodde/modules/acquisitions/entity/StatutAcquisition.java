package ong.fodde.modules.acquisitions.entity;

public enum StatutAcquisition {
    ATTENTE_COORDONNATEUR, // Visa du coordonnateur (chef de projet) attendu avant le RAF
    EN_ATTENTE,  // Soumise, en attente de validation RAF
    VALIDEE,     // Validée par le RAF
    REJETEE      // Rejetée (motif obligatoire)
}
