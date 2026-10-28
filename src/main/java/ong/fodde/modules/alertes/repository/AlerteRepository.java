package ong.fodde.modules.alertes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.alertes.entity.Alerte;

@Repository
public interface AlerteRepository extends JpaRepository<Alerte, UUID> {

    @Query("SELECT a FROM Alerte a WHERE a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Alerte> findAllActive();

    Optional<Alerte> findByReferenceKey(String referenceKey);

    @Query("SELECT a FROM Alerte a WHERE a.isDeleted = false AND a.lue = false")
    List<Alerte> findAllActiveNonLues();
}
