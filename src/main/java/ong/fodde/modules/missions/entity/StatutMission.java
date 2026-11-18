package ong.fodde.modules.missions.entity;

/**
 * Circuit d'une mission (novembre 2026) :
 * SOUMISE (chez le RAF : équipe, budget, validation) → EN_VALIDATION_SE (au-delà du seuil, ou à la
 * demande du RAF) → APPROUVEE_N1 (chez la logistique : véhicule, chauffeur) → VALIDEE (fonds à
 * débloquer par la comptabilité) → PRETE (ordre de mission).
 * Les étapes sans objet sont sautées (pas de besoin logistique, pas de frais).
 * ORGANISEE : ancienne étape « chez le RAF après la logistique », plus utilisée.
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
