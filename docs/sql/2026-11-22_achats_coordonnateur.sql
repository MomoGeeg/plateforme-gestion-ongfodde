-- Achats : le visa avant le RAF est donné par le coordonnateur du projet (chef de projet),
-- et non plus par le superviseur du périmètre. Les achats en attente de visa restent en attente.
ALTER TYPE statut_acquisition RENAME VALUE 'ATTENTE_SUPERVISEUR' TO 'ATTENTE_COORDONNATEUR';
UPDATE historique_demandes SET statut_avant = 'ATTENTE_COORDONNATEUR' WHERE statut_avant = 'ATTENTE_SUPERVISEUR';
UPDATE historique_demandes SET statut_apres = 'ATTENTE_COORDONNATEUR' WHERE statut_apres = 'ATTENTE_SUPERVISEUR';
