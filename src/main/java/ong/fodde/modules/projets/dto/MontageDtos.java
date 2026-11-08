package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import ong.fodde.modules.projets.entity.CategorieBudget;

/** Montage d'un projet : cadre logique, budget prévisionnel, équipe, contrôles avant soumission. */
public final class MontageDtos {

    private MontageDtos() {
    }

    public record ObjectifRequest(@NotBlank(message = "Indiquez l'objectif") String libelle, Integer ordre) {
    }

    public record ResultatRequest(@NotBlank(message = "Indiquez le résultat attendu") String libelle,
                                  UUID objectifId, Integer ordre) {
    }

    public record LigneBudgetRequest(String code, String intitule, CategorieBudget categorie, BigDecimal montant,
                                     UUID financementId) {
    }

    public record BudgetRequest(List<LigneBudgetRequest> lignes) {
    }

    public record ObjectifResponse(UUID id, String libelle, Integer ordre) {
    }

    public record ResultatResponse(UUID id, UUID objectifId, String libelle, Integer ordre) {
    }

    public record LigneBudgetResponse(UUID id, String code, String intitule, CategorieBudget categorie,
                                      BigDecimal montant, UUID financementId) {
    }

    public record IndicateurResume(UUID id, String code, String libelle, UUID resultatId, String unite,
                                   BigDecimal valeurBaseline, BigDecimal valeurCible) {
    }

    public record FinancementResume(UUID id, String bailleurNom, String referenceConvention,
                                    BigDecimal montantAccorde) {
    }

    public record MembreResponse(UUID userId, String nom, List<String> roles, boolean chefProjet) {
    }

    /** Compte actif, pour choisir un chef de projet, un responsable d'activité ou un membre. */
    public record PersonneResponse(UUID id, String nom, List<String> roles) {
    }

    /** Un contrôle avant soumission ; {@code bloquant} faux = simple information. */
    public record Controle(String libelle, boolean ok, boolean bloquant) {
    }

    /** Tout ce que la fiche projet affiche, en un appel. */
    public record MontageResponse(
            ProjetResponse projet,
            List<ObjectifResponse> objectifs,
            List<ResultatResponse> resultats,
            List<ActiviteResponse> activites,
            List<IndicateurResume> indicateurs,
            List<LigneBudgetResponse> budget,
            List<FinancementResume> financements,
            BigDecimal totalFinancements,
            List<MembreResponse> equipe,
            List<Controle> controles,
            /** L'utilisateur peut modifier le montage (projet en brouillon, RP, chef de projet ou admin). */
            boolean peutModifier) {
    }
}
