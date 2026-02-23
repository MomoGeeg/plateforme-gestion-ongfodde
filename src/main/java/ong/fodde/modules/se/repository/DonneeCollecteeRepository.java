package ong.fodde.modules.se.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.se.entity.DonneeCollectee;

@Repository
public interface DonneeCollecteeRepository extends JpaRepository<DonneeCollectee, UUID> {

    List<DonneeCollectee> findByFormulaireId(UUID formulaireId);

    List<DonneeCollectee> findByCollecteurId(UUID collecteurId);

    List<DonneeCollectee> findByFormulaireIdAndValide(UUID formulaireId, boolean valide);
}
