package ong.fodde.modules.rh.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.rh.dto.EvaluationRequest;
import ong.fodde.modules.rh.dto.EvaluationResponse;
import ong.fodde.modules.rh.entity.Evaluation;
import ong.fodde.modules.rh.entity.StatutEval;
import ong.fodde.modules.rh.repository.EvaluationRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;

    public EvaluationService(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    public EvaluationResponse creerEvaluation(EvaluationRequest request) {
        Evaluation evaluation = new Evaluation();
        evaluation.setEmployeId(request.employeId());
        evaluation.setEvaluateurId(request.evaluateurId());
        evaluation.setPeriode(request.periode());
        evaluation.setCommentaires(request.commentaires());
        evaluation.setPointsForts(request.pointsForts());
        evaluation.setAxesAmelioration(request.axesAmelioration());
        evaluation.setDateEvaluation(request.dateEvaluation());
        evaluation.setNoteGlobale(request.noteGlobale());
        evaluation.setStatut(StatutEval.EN_COURS);

        Evaluation saved = evaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    public EvaluationResponse soumettreEvaluation(UUID id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation non trouvée avec l'id : " + id));

        if (evaluation.getStatut() != StatutEval.EN_COURS) {
            throw new BusinessException("Seules les évaluations en cours peuvent être soumises");
        }

        evaluation.setStatut(StatutEval.SOUMISE);
        Evaluation saved = evaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    public EvaluationResponse validerEvaluation(UUID id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation non trouvée avec l'id : " + id));

        if (evaluation.getStatut() != StatutEval.SOUMISE) {
            throw new BusinessException("Seules les évaluations soumises peuvent être validées");
        }

        evaluation.setStatut(StatutEval.VALIDEE);
        Evaluation saved = evaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    public EvaluationResponse confirmerLecture(UUID id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation non trouvée avec l'id : " + id));

        if (evaluation.getStatut() != StatutEval.VALIDEE) {
            throw new BusinessException("Seules les évaluations validées peuvent être marquées comme lues");
        }

        evaluation.setStatut(StatutEval.LUE);
        Evaluation saved = evaluationRepository.save(evaluation);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> listerEvaluationsEmploye(UUID employeId) {
        return evaluationRepository.findByEmployeId(employeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> listerEvaluationsEvaluateur(UUID evaluateurId) {
        return evaluationRepository.findByEvaluateurId(evaluateurId).stream()
                .map(this::toResponse)
                .toList();
    }

    private EvaluationResponse toResponse(Evaluation e) {
        return new EvaluationResponse(
                e.getId(),
                e.getEmployeId(),
                e.getEvaluateurId(),
                e.getPeriode(),
                e.getStatut(),
                e.getNoteGlobale(),
                e.getCommentaires(),
                e.getPointsForts(),
                e.getAxesAmelioration(),
                e.getDateEvaluation(),
                null, // dateLectureEmploye — champ non présent dans l'entité actuelle
                e.getCreatedAt()
        );
    }
}
