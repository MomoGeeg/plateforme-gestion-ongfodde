package ong.fodde.modules.rh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import ong.fodde.modules.rh.entity.StatutEval;

public record EvaluationResponse(
        UUID id,
        UUID employeId,
        UUID evaluateurId,
        String periode,
        StatutEval statut,
        BigDecimal noteGlobale,
        String commentaires,
        String pointsForts,
        String axesAmelioration,
        LocalDate dateEvaluation,
        LocalDateTime dateLectureEmploye,
        LocalDateTime createdAt
) {
}
