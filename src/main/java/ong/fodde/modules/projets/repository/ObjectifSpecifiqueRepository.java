package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ong.fodde.modules.projets.entity.ObjectifSpecifique;

public interface ObjectifSpecifiqueRepository extends JpaRepository<ObjectifSpecifique, UUID> {
    List<ObjectifSpecifique> findByProjetIdOrderByOrdreAsc(UUID projetId);
}
