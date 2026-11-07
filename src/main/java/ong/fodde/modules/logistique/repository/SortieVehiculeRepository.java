package ong.fodde.modules.logistique.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.logistique.entity.SortieVehicule;

@Repository
public interface SortieVehiculeRepository extends JpaRepository<SortieVehicule, UUID> {

    List<SortieVehicule> findByVehiculeIdOrderByDateDepartDesc(UUID vehiculeId);

    /** Sortie en cours (pas encore revenue). */
    Optional<SortieVehicule> findFirstByVehiculeIdAndDateRetourIsNull(UUID vehiculeId);

    /** Toutes les sorties en cours, pour la liste du parc. */
    List<SortieVehicule> findByDateRetourIsNull();
}
