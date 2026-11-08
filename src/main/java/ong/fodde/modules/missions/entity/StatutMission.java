package ong.fodde.modules.missions.entity;

/**
 * Circuit d'une mission :
 * SOUMISE (chez le supérieur direct) → APPROUVEE_N1 (à organiser par la logistique)
 * → ORGANISEE (chez le RAF : équipe, budget) → EN_VALIDATION_SE (si nécessaire)
 * → VALIDEE (fonds à débloquer par la comptabilité) → PRETE (ordre de mission).
 * Les étapes sans objet sont sautées (pas de supérieur, pas de besoin logistique, pas de frais).
 */
public enum StatutMission {
    SOUMISE,
    APPROUVEE_N1,
    ORGANISEE,
    EN_VALIDATION_SE,
    VALIDEE,
    PRETE,
    REJETEE,
    ANNULEE,
    /** Ancien circuit (avant novembre 2026) : mission validée par le RAF. */
    VALIDEE_RAF
}
