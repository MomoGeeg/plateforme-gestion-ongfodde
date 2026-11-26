-- Plannings et rapports du terrain : après le visa du superviseur, le document passe chez le
-- coordonnateur du projet (statut TRANSMIS), qui le valide ou le renvoie à son auteur.
-- Validé, il est visible du RAF, du RP, du SE et du suivi-évaluation.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.

ALTER TABLE suivis_mensuels DROP CONSTRAINT IF EXISTS suivis_mensuels_statut_check;
ALTER TABLE suivis_mensuels ADD CONSTRAINT suivis_mensuels_statut_check
    CHECK (statut IN ('SOUMIS', 'TRANSMIS', 'VALIDE', 'A_REVOIR'));

-- Visa du superviseur.
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS vise_par UUID REFERENCES users(id);
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS date_visa TIMESTAMP;

-- Texte remis par l'auteur, gardé quand le superviseur le corrige.
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS contenu_initial TEXT;

CREATE INDEX IF NOT EXISTS idx_suivis_mensuels_statut ON suivis_mensuels(statut) WHERE is_deleted = false;
