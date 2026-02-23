package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.DemandeActivite;
import ong.fodde.modules.projets.entity.StatutDemandeActivite;

@Repository
public interface DemandeActiviteRepository extends JpaRepository<DemandeActivite, UUID> {

    List<DemandeActivite> findByProjetId(UUID projetId);

    List<DemandeActivite> findByDemandeurId(UUID demandeurId);

    List<DemandeActivite> findByStatut(StatutDemandeActivite statut);
}
