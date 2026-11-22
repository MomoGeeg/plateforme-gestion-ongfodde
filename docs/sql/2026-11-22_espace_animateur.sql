-- Espace animateur : rôle, rattachement aux projets, activités de sensibilisation et de suivi,
-- fiche de suivi, liste de présence (CSV) et photos des activités.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.

INSERT INTO roles (nom, description)
SELECT 'ROLE_ANIMATEUR', 'Animateur de terrain'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nom = 'ROLE_ANIMATEUR');

-- Projets sur lesquels intervient un animateur (le coordonnateur du projet est son supérieur).
CREATE TABLE IF NOT EXISTS animateurs_projet (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projet_id     UUID NOT NULL REFERENCES projets(id),
    animateur_id  UUID NOT NULL REFERENCES users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    is_deleted    BOOLEAN NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_animateurs_projet
    ON animateurs_projet(projet_id, animateur_id) WHERE is_deleted = false;

ALTER TABLE activites ADD COLUMN IF NOT EXISTS lieu VARCHAR(200);
ALTER TABLE activites ADD COLUMN IF NOT EXISTS fiche_suivi TEXT;

-- Pièces d'une activité : une liste de présence (CSV) et jusqu'à 3 photos.
CREATE TABLE IF NOT EXISTS pieces_activite (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activite_id   UUID NOT NULL REFERENCES activites(id),
    type_piece    VARCHAR(20) NOT NULL CHECK (type_piece IN ('LISTE_PRESENCE', 'PHOTO')),
    nom           VARCHAR(255) NOT NULL,
    chemin        TEXT NOT NULL,
    type_fichier  VARCHAR(20),
    taille        BIGINT,
    ajoute_par    UUID REFERENCES users(id),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP,
    is_deleted    BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_pieces_activite_activite ON pieces_activite(activite_id);
