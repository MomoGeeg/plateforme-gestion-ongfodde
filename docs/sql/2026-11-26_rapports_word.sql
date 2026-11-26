-- Rapports de terrain en fichier Word, rattachés à un projet ; coordonnateur obligatoire.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.

-- Chaque projet a un coordonnateur (chargé de projet) : il valide le suivi du terrain.
ALTER TABLE projets ALTER COLUMN chef_projet_id SET NOT NULL;

-- Planning ou rapport remis pour un projet donné (un périmètre n'appartient qu'à un projet).
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS projet_id UUID REFERENCES projets(id);
ALTER TABLE suivis_mensuels ALTER COLUMN projet_id SET NOT NULL;
DROP INDEX IF EXISTS uq_suivis_mensuels;
CREATE UNIQUE INDEX uq_suivis_mensuels
    ON suivis_mensuels(technicien_id, type_suivi, mois, projet_id) WHERE is_deleted = false;

-- Le rapport est un fichier Word ; le texte devient un résumé facultatif.
ALTER TABLE suivis_mensuels ALTER COLUMN contenu DROP NOT NULL;
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS fichier_nom VARCHAR(255);
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS fichier_chemin VARCHAR(500);
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS fichier_taille BIGINT;
-- Fichier remis par l'auteur, gardé quand le superviseur dépose une version corrigée.
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS fichier_initial_nom VARCHAR(255);
ALTER TABLE suivis_mensuels ADD COLUMN IF NOT EXISTS fichier_initial_chemin VARCHAR(500);
