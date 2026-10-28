package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.LigneBudgetaire;

@Repository
public interface LigneBudgetaireRepository extends JpaRepository<LigneBudgetaire, UUID> {

    List<LigneBudgetaire> findByAllocationProjetId(UUID allocationProjetId);

    Optional<LigneBudgetaire> findByAllocationProjetIdAndCode(UUID allocationProjetId, String code);
}
