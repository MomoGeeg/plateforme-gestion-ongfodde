package ong.fodde.modules.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ong.fodde.modules.auth.entity.Connexion;

public interface ConnexionRepository extends JpaRepository<Connexion, UUID> {

    List<Connexion> findTop1000ByDateConnexionGreaterThanEqualOrderByDateConnexionDesc(LocalDateTime depuis);

    List<Connexion> findTop1000ByUserIdAndDateConnexionGreaterThanEqualOrderByDateConnexionDesc(
            UUID userId, LocalDateTime depuis);

    @Modifying
    @Query("delete from Connexion c where c.dateConnexion < :limite")
    int supprimerAvant(@Param("limite") LocalDateTime limite);
}
