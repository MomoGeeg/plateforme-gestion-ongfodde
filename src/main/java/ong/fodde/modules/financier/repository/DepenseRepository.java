package ong.fodde.modules.financier.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.StatutDepense;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, UUID> {

    List<Depense> findByProjetId(UUID projetId);

    List<Depense> findByStatut(StatutDepense statut);

    List<Depense> findByLigneBudgetaireId(UUID ligneId);

    List<Depense> findBySaisiParId(UUID userId);

    @Query("SELECT COALESCE(SUM(d.montant), 0) FROM Depense d WHERE d.statut = 'VALIDEE' AND d.dateDepense >= :debut AND d.dateDepense <= :fin")
    BigDecimal sumByPeriode(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
