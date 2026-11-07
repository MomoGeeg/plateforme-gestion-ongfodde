package ong.fodde.modules.logistique.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.logistique.entity.MouvementMateriel;

@Repository
public interface MouvementMaterielRepository extends JpaRepository<MouvementMateriel, UUID> {

    List<MouvementMateriel> findByMaterielIdOrderByDateMouvementDescCreatedAtDesc(UUID materielId);
}
