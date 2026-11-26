package ong.fodde.modules.projets.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ong.fodde.modules.projets.entity.StatutActivite;
import ong.fodde.modules.projets.entity.StatutSuivi;
import ong.fodde.modules.projets.entity.TypeSuivi;

/** Planning et rapport mensuels du technicien ; équipe du superviseur ; rapports validés. */
public final class SuiviDtos {

    private SuiviDtos() {
    }

    public record SuiviRequest(
            @NotNull TypeSuivi type,
            @NotBlank @Pattern(regexp = "[0-9]{4}-(0[1-9]|1[0-2])", message = "Mois au format AAAA-MM") String mois,
            @NotBlank String contenu) {
    }

    /** `contenu` : texte corrigé par le superviseur au moment du visa (facultatif). */
    public record DecisionRequest(@NotNull Boolean valide, String commentaire, String contenu) {

        public DecisionRequest(Boolean valide, String commentaire) {
            this(valide, commentaire, null);
        }
    }

    /** Activité prévue dans le mois, rappelée sous le planning. */
    public record ActiviteDuMois(UUID id, String titre, LocalDate datePrevuDebut, String perimetreNom,
                                 StatutActivite statut) {
    }

    public record SuiviResponse(UUID id, TypeSuivi type, String mois, UUID technicienId, String technicienNom,
                                String contenu, StatutSuivi statut, String commentaire, String decideParNom,
                                LocalDateTime dateDecision, LocalDateTime createdAt,
                                List<ActiviteDuMois> activites,
                                String viseParNom, LocalDateTime dateVisa, String contenuInitial,
                                List<String> projets, boolean aDecider) {
    }

    /** Un technicien encadré, avec l'avancement de ses activités et l'état de son suivi mensuel. */
    public record MembreEquipe(UUID technicienId, String nom, List<String> perimetres,
                               int activitesPrevues, int activitesRealisees, int enRetard,
                               int formations, int personnesFormees,
                               StatutSuivi planningDuMois, StatutSuivi dernierRapport, String moisDernierRapport) {
    }
}
