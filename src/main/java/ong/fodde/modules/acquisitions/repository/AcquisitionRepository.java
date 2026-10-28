package ong.fodde.modules.acquisitions.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.acquisitions.entity.Acquisition;
import ong.fodde.modules.acquisitions.entity.StatutAcquisition;

@Repository
public interface AcquisitionRepository extends JpaRepository<Acquisition, UUID> {

    List<Acquisition> findByProjetId(UUID projetId);

    List<Acquisition> findByStatut(StatutAcquisition statut);
}
