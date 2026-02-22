package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.Budget;
import ong.fodde.modules.financier.entity.StatutBudget;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Optional<Budget> findByProjetId(UUID projetId);

    List<Budget> findByStatut(StatutBudget statut);
}
