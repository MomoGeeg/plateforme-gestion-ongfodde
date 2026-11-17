-- Heure de déconnexion dans l'historique des connexions.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.
-- Vide tant que la session est ouverte, ou si elle a expiré sans déconnexion.
ALTER TABLE historique_connexions ADD COLUMN IF NOT EXISTS date_deconnexion TIMESTAMP;
