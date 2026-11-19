package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.MesurePerimetre;

@Repository
public interface MesurePerimetreRepository extends JpaRepository<MesurePerimetre, UUID> {

    List<MesurePerimetre> findByPerimetreIdAndIsDeletedFalseOrderByDateMesureDescCreatedAtDesc(UUID perimetreId);
}
