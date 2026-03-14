package ong.fodde.modules.ged.entity;

public enum TypeDocument {
    // ── Financier (RAF / Comptable) ─────────────────────
    JUSTIFICATIF,
    FACTURE,
    RAPPORT_FINANCIER,
    CONTRAT_PRESTATAIRE,
    ACQUISITION,

    // ── RH ──────────────────────────────────────────────
    CONTRAT_EMPLOI,
    BULLETIN_SALAIRE,
    EVALUATION_RH,

    // ── Programmes / S&E ────────────────────────────────
    RAPPORT_ACTIVITE,
    RAPPORT_MISSION,
    PLAN_TRAVAIL,
    RAPPORT_SUIVI,
    RAPPORT_EVALUATION,
    PLAN_SE,
    DONNEES_COLLECTE,
    PHOTO_TERRAIN,

    // ── Institutionnel / Direction ───────────────────────
    CONVENTION_BAILLEUR,
    INSTITUTIONNEL,
    PV_REUNION,

    // ── Générique ───────────────────────────────────────
    AUTRE
}
