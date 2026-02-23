package ong.fodde.modules.se.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.se.entity.FormulaireCollecte;
import ong.fodde.modules.se.entity.StatutFormulaire;

@Repository
public interface FormulaireCollecteRepository extends JpaRepository<FormulaireCollecte, UUID> {

    List<FormulaireCollecte> findByProjetId(UUID projetId);

    List<FormulaireCollecte> findByStatut(StatutFormulaire statut);
}
