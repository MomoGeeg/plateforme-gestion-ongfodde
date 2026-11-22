package ong.fodde.modules.acquisitions.entity;

public enum StatutAcquisition {
        EN_ATTENTE,  // Soumise, en attente de validation RAF
    ATTENTE_RP,  // Transmise par le RAF au responsable des programmes
    VALIDEE,     // Validée par le RAF ou le RP
    REJETEE      // Rejetée (motif obligatoire)
}
