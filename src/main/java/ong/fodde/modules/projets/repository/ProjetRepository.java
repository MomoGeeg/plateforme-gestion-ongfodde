package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;

@Repository
public interface ProjetRepository extends JpaRepository<Projet, UUID> {

    List<Projet> findByStatut(StatutProjet statut);

    List<Projet> findByResponsableId(UUID responsableId);

    boolean existsByCode(String code);

    // Projets où l'utilisateur est membre de l'équipe
    @Query("SELECT p FROM Projet p JOIN p.equipe m WHERE m.id = :userId")
    List<Projet> findByMembreEquipe(@Param("userId") UUID userId);

    // Projets où l'utilisateur est responsable OU membre équipe
    @Query("SELECT DISTINCT p FROM Projet p LEFT JOIN p.equipe m WHERE p.responsableId = :userId OR m.id = :userId")
    List<Projet> findByResponsableOrMembre(@Param("userId") UUID userId);
}