-- Périmètres : coordonnées géographiques (carte du RP et du SE) et cultures pratiquées.
ALTER TABLE perimetres ADD COLUMN IF NOT EXISTS latitude NUMERIC(9, 6);
ALTER TABLE perimetres ADD COLUMN IF NOT EXISTS longitude NUMERIC(9, 6);
ALTER TABLE perimetres ADD COLUMN IF NOT EXISTS cultures VARCHAR(500);
