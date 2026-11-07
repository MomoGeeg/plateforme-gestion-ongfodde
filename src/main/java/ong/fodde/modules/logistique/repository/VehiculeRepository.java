package ong.fodde.modules.logistique.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.logistique.entity.Vehicule;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, UUID> {

    List<Vehicule> findAllByOrderByImmatriculationAsc();

    boolean existsByImmatriculationIgnoreCase(String immatriculation);

    boolean existsByImmatriculationIgnoreCaseAndIdNot(String immatriculation, UUID id);
}
