-- Espace superviseur : visa des achats de terrain, planning et rapport mensuels des techniciens.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.

-- Un achat demandé pour un périmètre passe d'abord par le superviseur du périmètre.
ALTER TYPE statut_acquisition ADD VALUE IF NOT EXISTS 'ATTENTE_SUPERVISEUR' BEFORE 'EN_ATTENTE';

-- Planning (activités prévues du mois) et rapport mensuel d'un technicien, validés par son superviseur.
CREATE TABLE IF NOT EXISTS suivis_mensuels (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type_suivi     VARCHAR(20) NOT NULL CHECK (type_suivi IN ('PLANNING', 'RAPPORT')),
    technicien_id  UUID NOT NULL REFERENCES users(id),
    mois           VARCHAR(7) NOT NULL,
    contenu        TEXT NOT NULL,
    statut         VARCHAR(20) NOT NULL DEFAULT 'SOUMIS' CHECK (statut IN ('SOUMIS', 'VALIDE', 'A_REVOIR')),
    commentaire    TEXT,
    decide_par     UUID REFERENCES users(id),
    date_decision  TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP,
    is_deleted     BOOLEAN NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_suivis_mensuels
    ON suivis_mensuels(technicien_id, type_suivi, mois) WHERE is_deleted = false;
