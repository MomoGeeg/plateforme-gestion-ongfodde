package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.Financement;

@Repository
public interface FinancementRepository extends JpaRepository<Financement, UUID> {

    List<Financement> findByProjetId(UUID projetId);

    List<Financement> findByBailleurId(UUID bailleurId);
}
