package ong.fodde.modules.rh.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.rh.entity.Evaluation;
import ong.fodde.modules.rh.entity.StatutEval;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

    List<Evaluation> findByEmployeId(UUID employeId);

    List<Evaluation> findByEvaluateurId(UUID evaluateurId);

    List<Evaluation> findByStatut(StatutEval statut);
}
