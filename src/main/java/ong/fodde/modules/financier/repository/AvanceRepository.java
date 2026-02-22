package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.Avance;
import ong.fodde.modules.financier.entity.StatutAvance;

@Repository
public interface AvanceRepository extends JpaRepository<Avance, UUID> {

    List<Avance> findByBeneficiaireId(UUID userId);

    List<Avance> findByProjetId(UUID projetId);

    List<Avance> findByStatut(StatutAvance statut);
}
