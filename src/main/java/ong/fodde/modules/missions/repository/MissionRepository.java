package ong.fodde.modules.missions.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.missions.entity.Mission;
import ong.fodde.modules.missions.entity.StatutMission;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {

    List<Mission> findByProjetId(UUID projetId);

    List<Mission> findByEmployeId(UUID employeId);

    List<Mission> findByStatut(StatutMission statut);
}
