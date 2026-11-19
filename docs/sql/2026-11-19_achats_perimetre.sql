-- Demande d'achat pour un périmètre (le projet est déduit du périmètre).
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.
ALTER TABLE acquisitions ADD COLUMN IF NOT EXISTS perimetre_id UUID REFERENCES perimetres(id);
CREATE INDEX IF NOT EXISTS idx_acquisitions_perimetre ON acquisitions(perimetre_id);
