package ong.fodde.modules.logistique.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.logistique.entity.MouvementMateriel;
import ong.fodde.modules.logistique.entity.TypeMouvement;

@Repository
public interface MouvementMaterielRepository extends JpaRepository<MouvementMateriel, UUID> {

    List<MouvementMateriel> findByIsDeletedFalseOrderByDateMouvementDesc();

    List<MouvementMateriel> findByBienIdAndIsDeletedFalseOrderByDateMouvementDesc(UUID bienId);

    List<MouvementMateriel> findByVehiculeIdAndIsDeletedFalseOrderByDateMouvementDesc(UUID vehiculeId);

    List<MouvementMateriel> findByProjetIdAndIsDeletedFalseOrderByDateMouvementDesc(UUID projetId);

    List<MouvementMateriel> findByTypeAndIsDeletedFalseOrderByDateMouvementDesc(TypeMouvement type);
}
