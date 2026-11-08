package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ong.fodde.modules.projets.entity.LigneBudgetProjet;

public interface LigneBudgetProjetRepository extends JpaRepository<LigneBudgetProjet, UUID> {
    List<LigneBudgetProjet> findByProjetIdOrderByOrdreAsc(UUID projetId);

    void deleteByProjetId(UUID projetId);
}
