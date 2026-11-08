package ong.fodde.modules.missions.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.missions.entity.Mission;
import ong.fodde.modules.missions.entity.StatutMission;

@Repository
public interface MissionRepository extends JpaRepository<Mission, UUID> {

    List<Mission> findByProjetId(UUID projetId);

    List<Mission> findByStatut(StatutMission statut);

    /** Missions dont la période chevauche [debut, fin], hors statuts exclus (rejetées, annulées). */
    @Query("select m from Mission m where m.statut not in :exclus and m.dateDebut <= :fin and m.dateFin >= :debut")
    List<Mission> chevauchant(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin,
                              @Param("exclus") Collection<StatutMission> exclus);
}
