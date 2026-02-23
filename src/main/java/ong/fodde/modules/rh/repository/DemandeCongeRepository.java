package ong.fodde.modules.rh.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.rh.entity.DemandeConge;
import ong.fodde.modules.rh.entity.StatutConge;

@Repository
public interface DemandeCongeRepository extends JpaRepository<DemandeConge, UUID> {

    List<DemandeConge> findByEmployeId(UUID employeId);

    List<DemandeConge> findByStatut(StatutConge statut);
}
