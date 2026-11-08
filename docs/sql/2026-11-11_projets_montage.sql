-- Montage des projets : cadre logique, budget prévisionnel, circuit RP → RAF → SE.
-- À appliquer dans Supabase AVANT de déployer le backend. Ajouts seulement.

-- Nouvelle étape : chez le SE (EN_VALIDATION = chez le RAF)
ALTER TYPE statut_projet ADD VALUE IF NOT EXISTS 'EN_VALIDATION_SE' AFTER 'EN_VALIDATION';

ALTER TABLE projets
    ADD COLUMN IF NOT EXISTS chef_projet_id UUID REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS motif_renvoi TEXT;

-- Objectifs spécifiques (sous l'objectif général)
CREATE TABLE IF NOT EXISTS objectifs_specifiques (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projet_id UUID NOT NULL REFERENCES projets(id) ON DELETE CASCADE,
    libelle TEXT NOT NULL,
    ordre INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_objectifs_projet ON objectifs_specifiques (projet_id);

-- Résultats attendus → objectif spécifique ; activités → résultat attendu
ALTER TABLE resultats_attendus
    ADD COLUMN IF NOT EXISTS objectif_id UUID REFERENCES objectifs_specifiques(id) ON DELETE SET NULL;
ALTER TABLE activites
    ADD COLUMN IF NOT EXISTS resultat_id UUID REFERENCES resultats_attendus(id);
CREATE INDEX IF NOT EXISTS idx_activites_resultat ON activites (resultat_id);

-- Budget prévisionnel du projet, sur toute sa durée
CREATE TABLE IF NOT EXISTS lignes_budget_projet (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projet_id UUID NOT NULL REFERENCES projets(id) ON DELETE CASCADE,
    code VARCHAR(30),
    intitule VARCHAR(300) NOT NULL,
    categorie VARCHAR(30) NOT NULL CHECK (categorie IN
        ('PERSONNEL', 'DEPLACEMENTS', 'EQUIPEMENT', 'ACTIVITES', 'FONCTIONNEMENT', 'FRAIS_ADMINISTRATIFS', 'AUTRE')),
    montant NUMERIC(18, 2) NOT NULL CHECK (montant > 0),
    financement_id UUID REFERENCES financements(id),
    ordre INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_lignes_budget_projet ON lignes_budget_projet (projet_id);
