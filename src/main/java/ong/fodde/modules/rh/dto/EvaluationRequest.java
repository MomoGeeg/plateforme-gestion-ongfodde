package ong.fodde.modules.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvaluationRequest(
        @NotNull(message = "L'identifiant de l'employé est obligatoire")
        UUID employeId,

        @NotNull(message = "L'identifiant de l'évaluateur est obligatoire")
        UUID evaluateurId,

        @NotBlank(message = "La période est obligatoire")
        String periode,

        String commentaires,
        String pointsForts,
        String axesAmelioration,
        LocalDate dateEvaluation,
        BigDecimal noteGlobale
) {
}
