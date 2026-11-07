-- Historique des validations des demandes (dépenses, missions, acquisitions, congés).
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.
CREATE TABLE IF NOT EXISTS historique_demandes (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type_demande  VARCHAR(20)  NOT NULL,          -- DEPENSE, MISSION, ACQUISITION, CONGE
  demande_id    UUID         NOT NULL,
  statut_avant  VARCHAR(30),                    -- vide pour la création
  statut_apres  VARCHAR(30)  NOT NULL,
  auteur_id     UUID,
  auteur_nom    VARCHAR(200),                   -- nom figé au moment de l'action
  motif         TEXT,
  date_action   TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_historique_demandes_demande
  ON historique_demandes (type_demande, demande_id, date_action);
-- Pas d'accès par l'API publique de Supabase ; le backend se connecte en propriétaire.
ALTER TABLE historique_demandes ENABLE ROW LEVEL SECURITY;
