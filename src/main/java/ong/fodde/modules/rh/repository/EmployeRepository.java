package ong.fodde.modules.rh.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.entity.StatutEmploye;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, UUID> {

    Optional<Employe> findByMatricule(String matricule);

    Optional<Employe> findByUserId(UUID userId);

    List<Employe> findByStatut(StatutEmploye statut);

    boolean existsByMatricule(String matricule);
}
