-- Achats : seul le coordonnateur du projet lance un achat, adressé au RAF ; le RAF peut le
-- transmettre au responsable des programmes (statut ATTENTE_RP). Plus de visa avant le RAF.
-- Aucun achat n'était au statut ATTENTE_COORDONNATEUR au moment du changement.
ALTER TYPE statut_acquisition RENAME VALUE 'ATTENTE_COORDONNATEUR' TO 'ATTENTE_RP';
UPDATE historique_demandes SET statut_avant = 'ATTENTE_RP' WHERE statut_avant = 'ATTENTE_COORDONNATEUR';
UPDATE historique_demandes SET statut_apres = 'ATTENTE_RP' WHERE statut_apres = 'ATTENTE_COORDONNATEUR';
