package ong.fodde.modules.logistique.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.logistique.entity.Materiel;

@Repository
public interface MaterielRepository extends JpaRepository<Materiel, UUID> {

    List<Materiel> findAllByOrderByCreatedAtDesc();

    Optional<Materiel> findByAcquisitionId(UUID acquisitionId);

    boolean existsByCodeActif(String codeActif);

    /** Code actif suivant, même format que la valeur par défaut de la colonne : MAT-2026-001. */
    @Query(value = "select 'MAT-' || to_char(now(), 'YYYY') || '-' || lpad(nextval('materiel_seq')::text, 3, '0')",
           nativeQuery = true)
    String prochainCodeActif();
}
