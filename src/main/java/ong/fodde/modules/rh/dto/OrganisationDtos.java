package ong.fodde.modules.rh.dto;

import java.util.UUID;

import ong.fodde.modules.rh.entity.Pole;

/** Réponses liées à l'organisation de l'ONG (pôles, supérieurs directs). */
public final class OrganisationDtos {

    private OrganisationDtos() {
    }

    /**
     * Rattachement proposé pour une personne, d'après les rôles de son compte.
     * {@code superieurId} est vide quand personne, ou plusieurs personnes, peuvent être le chef :
     * {@code message} l'explique et les RH choisissent.
     */
    public record PropositionRattachement(Pole pole, UUID superieurId, String superieurNom, String message) {
    }

    /** Une personne dans l'organigramme (employés actifs). */
    public record NoeudOrganigramme(UUID id, String nom, String prenom, String poste, Pole pole,
                                    UUID superieurId, boolean aUnCompte) {
    }
}
