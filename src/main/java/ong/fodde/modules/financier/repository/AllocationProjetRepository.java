package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.AllocationProjet;
import ong.fodde.modules.financier.entity.StatutAllocation;

@Repository
public interface AllocationProjetRepository extends JpaRepository<AllocationProjet, UUID> {

    List<AllocationProjet> findByBudgetAnnuelId(UUID budgetAnnuelId);

    List<AllocationProjet> findByStatut(StatutAllocation statut);

    Optional<AllocationProjet> findByBudgetAnnuelIdAndProjetId(UUID budgetAnnuelId, UUID projetId);

    List<AllocationProjet> findByProjetId(UUID projetId);
}
