package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.RapportActivite;
import ong.fodde.modules.projets.entity.StatutRapport;

@Repository
public interface RapportActiviteRepository extends JpaRepository<RapportActivite, UUID> {

    List<RapportActivite> findByProjetId(UUID projetId);

    List<RapportActivite> findByAuteurId(UUID auteurId);

    List<RapportActivite> findByStatut(StatutRapport statut);

    List<RapportActivite> findByProjetIdAndStatut(UUID projetId, StatutRapport statut);
}
