package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, UUID> {

    List<Projet> findByStatut(StatutProjet statut);

    List<Projet> findByResponsableId(UUID responsableId);

    boolean existsByCode(String code);
}
