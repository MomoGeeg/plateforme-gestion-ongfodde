package ong.fodde.modules.historique.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Une étape de l'historique d'une demande.
 *
 * @param statutAvant  statut quitté (vide pour la création)
 * @param statutApres  statut atteint
 * @param date         date de l'action ; vide si elle n'a pas été enregistrée
 *                     (demandes traitées avant la mise en place de l'historique)
 * @param reconstituee vrai si l'étape est déduite des champs de la demande
 *                     et non lue dans le journal
 */
public record EtapeHistorique(
    String statutAvant,
    String statutApres,
    UUID auteurId,
    String auteurNom,
    String motif,
    LocalDateTime date,
    boolean reconstituee
) {}
