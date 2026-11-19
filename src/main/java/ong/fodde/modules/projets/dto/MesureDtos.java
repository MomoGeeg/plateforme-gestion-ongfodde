package ong.fodde.modules.projets.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public final class MesureDtos {

    private MesureDtos() {
    }

    public record MesureRequest(
            @NotNull(message = "L'indicateur est obligatoire") UUID indicateurId,
            @NotNull(message = "La date est obligatoire") LocalDate dateMesure,
            @NotNull(message = "La valeur est obligatoire") BigDecimal valeur,
            String commentaire) {
    }

    public record MesureResponse(
            UUID id,
            UUID indicateurId,
            UUID perimetreId,
            LocalDate dateMesure,
            BigDecimal valeur,
            String commentaire,
            String saisiParNom) {
    }

    /** Un indicateur du projet vu depuis un périmètre : sa dernière valeur relevée. */
    public record IndicateurPerimetre(
            UUID indicateurId,
            String code,
            String libelle,
            String unite,
            BigDecimal valeurCible,
            BigDecimal derniereValeur,
            LocalDate derniereDate,
            int nbMesures) {
    }

    public record IndicateursDuPerimetre(
            UUID perimetreId,
            String perimetreNom,
            String village,
            boolean peutSaisir,
            List<IndicateurPerimetre> indicateurs) {
    }
}
