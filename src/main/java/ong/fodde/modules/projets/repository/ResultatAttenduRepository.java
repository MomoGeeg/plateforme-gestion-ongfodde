package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ong.fodde.modules.projets.entity.ResultatAttendu;

public interface ResultatAttenduRepository extends JpaRepository<ResultatAttendu, UUID> {
    List<ResultatAttendu> findByProjetIdOrderByOrdreAsc(UUID projetId);
}
