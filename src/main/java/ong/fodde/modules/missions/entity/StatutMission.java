package ong.fodde.modules.missions.entity;

public enum StatutMission {
    SOUMISE,        // Demande soumise par l'employé
    APPROUVEE_N1,   // Approuvée par le responsable hiérarchique
    VALIDEE_RAF,    // Validée par le RAF (décaissement autorisé)
    REJETEE         // Rejetée (motif obligatoire)
}
