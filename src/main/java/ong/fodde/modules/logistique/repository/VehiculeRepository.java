package ong.fodde.modules.logistique.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.logistique.entity.EtatVehicule;
import ong.fodde.modules.logistique.entity.Vehicule;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, UUID> {

    List<Vehicule> findByIsDeletedFalseOrderByCreatedAtDesc();

    List<Vehicule> findByEtatAndIsDeletedFalse(EtatVehicule etat);

    boolean existsByImmatriculationAndIsDeletedFalse(String immatriculation);

    long countByIsDeletedFalse();

    /**
     * Véhicules dont l'assurance expire dans les X prochains jours
     */
    @Query("SELECT v FROM Vehicule v WHERE v.isDeleted = false AND v.dateExpirationAssurance IS NOT NULL AND v.dateExpirationAssurance <= :seuil AND v.etat <> ong.fodde.modules.logistique.entity.EtatVehicule.HORS_SERVICE")
    List<Vehicule> findAssurancesExpirantAvant(@Param("seuil") LocalDate seuil);

    /**
     * Véhicules dont la visite technique expire dans les X prochains jours
     */
    @Query("SELECT v FROM Vehicule v WHERE v.isDeleted = false AND v.dateVisiteTechnique IS NOT NULL AND v.dateVisiteTechnique <= :seuil AND v.etat <> ong.fodde.modules.logistique.entity.EtatVehicule.HORS_SERVICE")
    List<Vehicule> findVisitesTechniquesExpirantAvant(@Param("seuil") LocalDate seuil);

    /**
     * Véhicules dont le kilométrage dépasse le seuil de révision
     */
    @Query("SELECT v FROM Vehicule v WHERE v.isDeleted = false AND v.kilometrageRevision IS NOT NULL AND v.kilometrageActuel >= v.kilometrageRevision AND v.etat <> ong.fodde.modules.logistique.entity.EtatVehicule.HORS_SERVICE")
    List<Vehicule> findVehiculesRevisionRequise();
}
