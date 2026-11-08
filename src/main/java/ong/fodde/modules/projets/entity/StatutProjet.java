package ong.fodde.modules.projets.entity;

/**
 * Cycle de vie d'un projet :
 * BROUILLON (montage par le RP et le chef de projet) → EN_VALIDATION (vérification du budget
 * par le RAF) → EN_VALIDATION_SE (validation du Secrétaire exécutif) → ACTIF
 * → SUSPENDU / EN_CLOTURE → CLOTURE → ARCHIVE.
 * Le RAF et le SE peuvent renvoyer le projet en brouillon, avec un motif.
 */
public enum StatutProjet {
    BROUILLON,
    EN_VALIDATION,
    EN_VALIDATION_SE,
    ACTIF,
    SUSPENDU,
    EN_CLOTURE,
    CLOTURE,
    ARCHIVE
}
