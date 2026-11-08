-- Missions refaites : équipe, logistique, budget détaillé, validation SE, déblocage des fonds.
-- À appliquer dans Supabase AVANT de déployer le backend. Ajouts seulement.

-- Nouvelles étapes du circuit (VALIDEE_RAF reste pour les missions de l'ancien circuit)
ALTER TYPE statut_mission ADD VALUE IF NOT EXISTS 'ORGANISEE';
ALTER TYPE statut_mission ADD VALUE IF NOT EXISTS 'EN_VALIDATION_SE';
ALTER TYPE statut_mission ADD VALUE IF NOT EXISTS 'VALIDEE';
ALTER TYPE statut_mission ADD VALUE IF NOT EXISTS 'PRETE';
ALTER TYPE statut_mission ADD VALUE IF NOT EXISTS 'ANNULEE';

ALTER TABLE missions
    ADD COLUMN IF NOT EXISTS n1_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS chef_mission_id UUID REFERENCES employes(id),
    ADD COLUMN IF NOT EXISTS besoin_vehicule BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS besoins_materiel TEXT,
    ADD COLUMN IF NOT EXISTS vehicule_id UUID REFERENCES vehicules(id),
    ADD COLUMN IF NOT EXISTS chauffeur_id UUID REFERENCES employes(id),
    ADD COLUMN IF NOT EXISTS notes_logistique TEXT,
    ADD COLUMN IF NOT EXISTS soumettre_se BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS avance_id UUID REFERENCES avances(id);

-- Équipe : fiches employés (un agent ou un chauffeur n'a pas forcément de compte)
CREATE TABLE IF NOT EXISTS mission_participants (
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    employe_id UUID NOT NULL REFERENCES employes(id),
    PRIMARY KEY (mission_id, employe_id)
);

-- Budget : lignes quantité × prix unitaire
CREATE TABLE IF NOT EXISTS mission_frais (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE CASCADE,
    ordre INTEGER NOT NULL,
    categorie VARCHAR(20) NOT NULL
        CHECK (categorie IN ('PER_DIEM', 'HEBERGEMENT', 'CARBURANT', 'TRANSPORT', 'AUTRE')),
    libelle VARCHAR(200),
    quantite NUMERIC(10, 2) NOT NULL CHECK (quantite > 0),
    prix_unitaire NUMERIC(18, 2) NOT NULL CHECK (prix_unitaire >= 0)
);

CREATE INDEX IF NOT EXISTS idx_mission_frais_mission ON mission_frais (mission_id);
CREATE INDEX IF NOT EXISTS idx_mission_participants_employe ON mission_participants (employe_id);
CREATE INDEX IF NOT EXISTS idx_missions_dates ON missions (date_debut, date_fin);
