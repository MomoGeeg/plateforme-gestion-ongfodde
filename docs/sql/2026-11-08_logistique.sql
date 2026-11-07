-- Logistique : compléments aux tables existantes (materiels, vehicules,
-- mouvements_materiel, sorties_vehicule), vides au moment de la migration.
-- À exécuter dans Supabase AVANT de déployer le backend qui les utilise.

-- Matériel : remarques, et lien vers l'acquisition validée d'où il vient
-- (une acquisition n'entre qu'une fois dans l'inventaire).
ALTER TABLE materiels
  ADD COLUMN IF NOT EXISTS notes          TEXT,
  ADD COLUMN IF NOT EXISTS acquisition_id UUID UNIQUE REFERENCES acquisitions(id);

-- Véhicules : carburant et remarques.
ALTER TABLE vehicules
  ADD COLUMN IF NOT EXISTS type_carburant VARCHAR(20),
  ADD COLUMN IF NOT EXISTS notes          TEXT;

-- Mouvements : responsable_id = la personne à qui le bien est affecté
-- (vide pour un retour, une réparation…) ; created_by = qui a saisi le mouvement.
ALTER TABLE mouvements_materiel
  ALTER COLUMN responsable_id DROP NOT NULL,
  ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);

-- Carnet de bord : qui a saisi la sortie.
ALTER TABLE sorties_vehicule
  ADD COLUMN IF NOT EXISTS created_by UUID REFERENCES users(id);

CREATE INDEX IF NOT EXISTS idx_mouvements_materiel_materiel ON mouvements_materiel (materiel_id, date_mvt);
CREATE INDEX IF NOT EXISTS idx_sorties_vehicule_vehicule   ON sorties_vehicule (vehicule_id, date_depart);
