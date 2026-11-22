package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.AnimateurProjet;

@Repository
public interface AnimateurProjetRepository extends JpaRepository<AnimateurProjet, UUID> {

    List<AnimateurProjet> findByProjetIdAndIsDeletedFalse(UUID projetId);

    List<AnimateurProjet> findByAnimateurIdAndIsDeletedFalse(UUID animateurId);

    Optional<AnimateurProjet> findByProjetIdAndAnimateurIdAndIsDeletedFalse(UUID projetId, UUID animateurId);

    boolean existsByProjetIdAndAnimateurIdAndIsDeletedFalse(UUID projetId, UUID animateurId);
}
