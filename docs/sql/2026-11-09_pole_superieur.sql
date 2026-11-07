-- Pôle et supérieur direct sur la fiche employé (organigramme de l'ONG).
-- À appliquer dans Supabase AVANT de déployer le backend.
-- Ajouts seulement : la colonne superieur_id existe déjà (clé étrangère vers employes) ;
-- l'ancienne colonne superviseur_id (vide, sans clé étrangère) n'est plus utilisée par le code
-- et pourra être supprimée plus tard.

ALTER TABLE employes ADD COLUMN IF NOT EXISTS pole VARCHAR(20);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'employes_pole_check') THEN
        ALTER TABLE employes ADD CONSTRAINT employes_pole_check
            CHECK (pole IN ('DIRECTION', 'RAF', 'PROGRAMMES', 'RED_CAP_COM'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_employes_superieur ON employes (superieur_id);

-- Rôles jamais attribués et inutilisés par le code : doublon du Secrétaire exécutif (ROLE_SE)
-- et rôle générique. Supprimés seulement s'ils ne sont attribués à personne.
DELETE FROM role_permissions rp
USING roles r
WHERE rp.role_id = r.id
  AND r.nom IN ('ROLE_SECRETAIRE_EXECUTIF', 'ROLE_USER')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.role_id = r.id);

DELETE FROM roles r
WHERE r.nom IN ('ROLE_SECRETAIRE_EXECUTIF', 'ROLE_USER')
  AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.role_id = r.id);
