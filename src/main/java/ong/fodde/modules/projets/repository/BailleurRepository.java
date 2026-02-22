package ong.fodde.modules.projets.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.Bailleur;

@Repository
public interface BailleurRepository extends JpaRepository<Bailleur, UUID> {

    Optional<Bailleur> findByNom(String nom);
}
