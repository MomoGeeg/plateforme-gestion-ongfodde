package ong.fodde.modules.acquisitions.entity;

public enum StatutAcquisition {
    ATTENTE_SUPERVISEUR, // Achat de terrain : visa du superviseur du périmètre attendu
    EN_ATTENTE,  // Soumise, en attente de validation RAF
    VALIDEE,     // Validée par le RAF
    REJETEE      // Rejetée (motif obligatoire)
}
