-- Périmètres maraîchers, rôle superviseur, formations.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.

INSERT INTO roles (nom, description)
SELECT 'ROLE_SUPERVISEUR', 'Superviseur des techniciens terrain'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nom = 'ROLE_SUPERVISEUR');

-- Un périmètre est situé dans un village, lié à un seul projet, un technicien et son superviseur.
CREATE TABLE IF NOT EXISTS perimetres (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projet_id       UUID NOT NULL REFERENCES projets(id),
    nom             VARCHAR(200) NOT NULL,
    village         VARCHAR(200) NOT NULL,
    commune         VARCHAR(150),
    departement     VARCHAR(150),
    superficie_ha   NUMERIC(10, 2),
    technicien_id   UUID REFERENCES users(id),
    superviseur_id  UUID REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP,
    is_deleted      BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_perimetres_projet ON perimetres(projet_id);
CREATE INDEX IF NOT EXISTS idx_perimetres_technicien ON perimetres(technicien_id);
CREATE INDEX IF NOT EXISTS idx_perimetres_superviseur ON perimetres(superviseur_id);

-- Une formation est une activité de type FORMATION, menée sur un périmètre.
ALTER TABLE activites ADD COLUMN IF NOT EXISTS type_activite VARCHAR(30) NOT NULL DEFAULT 'ACTIVITE';
ALTER TABLE activites ADD COLUMN IF NOT EXISTS perimetre_id UUID REFERENCES perimetres(id);
ALTER TABLE activites ADD COLUMN IF NOT EXISTS participants INTEGER;
ALTER TABLE activites ADD COLUMN IF NOT EXISTS participants_femmes INTEGER;
CREATE INDEX IF NOT EXISTS idx_activites_perimetre ON activites(perimetre_id);
