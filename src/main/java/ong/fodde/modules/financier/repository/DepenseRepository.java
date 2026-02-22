package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.StatutDepense;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, UUID> {

    List<Depense> findByProjetId(UUID projetId);

    List<Depense> findByStatut(StatutDepense statut);

    List<Depense> findByLigneBudgetaireId(UUID ligneId);

    List<Depense> findBySaisiParId(UUID userId);
}
