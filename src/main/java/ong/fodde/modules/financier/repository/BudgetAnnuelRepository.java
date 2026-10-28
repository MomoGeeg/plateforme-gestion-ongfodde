package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.BudgetAnnuel;

@Repository
public interface BudgetAnnuelRepository extends JpaRepository<BudgetAnnuel, UUID> {

    Optional<BudgetAnnuel> findByExercice(Integer exercice);

    List<BudgetAnnuel> findAllByOrderByExerciceDesc();
}
