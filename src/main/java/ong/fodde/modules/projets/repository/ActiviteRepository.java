package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.StatutActivite;

@Repository
public interface ActiviteRepository extends JpaRepository<Activite, UUID> {

    List<Activite> findByProjetId(UUID projetId);

    List<Activite> findByStatut(StatutActivite statut);
}
