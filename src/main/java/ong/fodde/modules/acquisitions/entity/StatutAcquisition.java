package ong.fodde.modules.acquisitions.entity;

public enum StatutAcquisition {
    EN_ATTENTE,  // Soumise, en attente de validation RAF
    VALIDEE,     // Validée par le RAF
    REJETEE      // Rejetée (motif obligatoire)
}
