-- Planification d'une activité (superviseur, et tous ceux qui planifient) : cible, participants
-- attendus, objectifs et indicateurs. Les présents restent comptés par la liste de présence.
ALTER TABLE activites ADD COLUMN IF NOT EXISTS cible VARCHAR(300);
ALTER TABLE activites ADD COLUMN IF NOT EXISTS participants_prevus INTEGER;
ALTER TABLE activites ADD COLUMN IF NOT EXISTS objectifs TEXT;
ALTER TABLE activites ADD COLUMN IF NOT EXISTS indicateurs TEXT;
