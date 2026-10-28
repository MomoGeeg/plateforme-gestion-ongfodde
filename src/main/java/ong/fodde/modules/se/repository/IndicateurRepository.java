package ong.fodde.modules.se.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.se.entity.Indicateur;

@Repository
public interface IndicateurRepository extends JpaRepository<Indicateur, UUID> {

    List<Indicateur> findByProjetId(UUID projetId);

    Optional<Indicateur> findByCode(String code);

    boolean existsByCode(String code);
}
