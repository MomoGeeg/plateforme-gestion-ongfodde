package ong.fodde.modules.se.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.se.entity.ChampFormulaire;

@Repository
public interface ChampFormulaireRepository extends JpaRepository<ChampFormulaire, UUID> {

    List<ChampFormulaire> findByFormulaireId(UUID formulaireId);

    long countByFormulaireId(UUID formulaireId);
}
