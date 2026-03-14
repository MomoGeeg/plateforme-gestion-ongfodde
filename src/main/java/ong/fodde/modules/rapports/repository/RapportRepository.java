package ong.fodde.modules.rapports.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.rapports.entity.Rapport;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, UUID> {

    List<Rapport> findAllByOrderByDateGenerationDesc();
}
