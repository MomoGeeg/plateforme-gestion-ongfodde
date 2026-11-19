-- Indicateurs relevés par périmètre (valeur d'un indicateur du projet sur un périmètre, à une date).
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.
CREATE TABLE IF NOT EXISTS mesures_perimetre (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    indicateur_id  UUID NOT NULL REFERENCES indicateurs(id),
    perimetre_id   UUID NOT NULL REFERENCES perimetres(id),
    date_mesure    DATE NOT NULL,
    valeur         NUMERIC(18, 2) NOT NULL,
    commentaire    TEXT,
    saisi_par      UUID REFERENCES users(id),
    created_at     TIMESTAMP NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP,
    is_deleted     BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_mesures_perimetre_perimetre ON mesures_perimetre(perimetre_id);
CREATE INDEX IF NOT EXISTS idx_mesures_perimetre_indicateur ON mesures_perimetre(indicateur_id);
