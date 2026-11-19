package ong.fodde.modules.auth.repository;

import ong.fodde.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Comptes ayant ce rôle (ex. ROLE_TECHNICIEN_TERRAIN), par nom. */
    List<User> findDistinctByRoles_NomOrderByNomAscPrenomAsc(String nom);
}
