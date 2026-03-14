package ong.fodde.modules.logistique.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.logistique.entity.Bien;
import ong.fodde.modules.logistique.entity.CategorieEquipement;
import ong.fodde.modules.logistique.entity.EtatBien;

@Repository
public interface BienRepository extends JpaRepository<Bien, UUID> {

    List<Bien> findByIsDeletedFalseOrderByCreatedAtDesc();

    List<Bien> findByCategorieAndIsDeletedFalse(CategorieEquipement categorie);

    List<Bien> findByEtatAndIsDeletedFalse(EtatBien etat);

    List<Bien> findByProjetIdAndIsDeletedFalse(UUID projetId);

    Optional<Bien> findByCodeActif(String codeActif);

    boolean existsByCodeActif(String codeActif);

    long countByIsDeletedFalse();

    long countByEtatAndIsDeletedFalse(EtatBien etat);
}
