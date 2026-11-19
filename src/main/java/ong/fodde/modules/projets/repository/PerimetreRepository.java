package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.Perimetre;

@Repository
public interface PerimetreRepository extends JpaRepository<Perimetre, UUID> {

    List<Perimetre> findByProjetIdAndIsDeletedFalseOrderByNomAsc(UUID projetId);

    List<Perimetre> findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(UUID technicienId);

    List<Perimetre> findBySuperviseurIdAndIsDeletedFalseOrderByNomAsc(UUID superviseurId);
}
