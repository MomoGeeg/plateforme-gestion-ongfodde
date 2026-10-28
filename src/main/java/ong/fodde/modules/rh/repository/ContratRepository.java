package ong.fodde.modules.rh.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.rh.entity.Contrat;
import ong.fodde.modules.rh.entity.StatutContrat;

@Repository
public interface ContratRepository extends JpaRepository<Contrat, UUID> {

    List<Contrat> findByEmployeId(UUID employeId);

    List<Contrat> findByStatut(StatutContrat statut);

    Optional<Contrat> findByEmployeIdAndStatut(UUID employeId, StatutContrat statut);
}
