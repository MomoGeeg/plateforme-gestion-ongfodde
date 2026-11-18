-- Circuit des congés : l'employé demande, les RH valident, puis le RAF.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.
ALTER TYPE statut_conge ADD VALUE IF NOT EXISTS 'VALIDE_RH';
ALTER TABLE demandes_conge ADD COLUMN IF NOT EXISTS valide_par_rh UUID;
ALTER TABLE demandes_conge ADD COLUMN IF NOT EXISTS date_validation_rh TIMESTAMP;
