-- Historique des connexions (réussies et refusées), consulté par l'administrateur.
-- À exécuter dans Supabase AVANT de déployer le backend qui l'utilise.
-- Pas de clé étrangère vers users : l'historique reste lisible après la suppression
-- d'un compte et n'empêche pas cette suppression.
CREATE TABLE IF NOT EXISTS historique_connexions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID,                          -- vide si l'e-mail ne correspond à aucun compte
  email           VARCHAR(255) NOT NULL,         -- e-mail saisi
  nom_complet     VARCHAR(200),                  -- nom figé au moment de la connexion
  succes          BOOLEAN      NOT NULL,
  motif_echec     VARCHAR(30),                   -- MOT_DE_PASSE, COMPTE_INACTIF, COMPTE_VERROUILLE, COMPTE_INCONNU
  adresse_ip      VARCHAR(64),
  navigateur      VARCHAR(300),
  date_connexion  TIMESTAMP    NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_historique_connexions_date ON historique_connexions (date_connexion DESC);
CREATE INDEX IF NOT EXISTS idx_historique_connexions_user ON historique_connexions (user_id, date_connexion DESC);
-- Pas d'accès par l'API publique de Supabase ; le backend se connecte en propriétaire.
ALTER TABLE historique_connexions ENABLE ROW LEVEL SECURITY;
