package ong.fodde.modules.ged.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.ged.entity.Document;
import ong.fodde.modules.ged.entity.StatutDocument;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByStatut(StatutDocument statut);

    List<Document> findByProjetId(UUID projetId);

    List<Document> findByStatutOrderByCreatedAtDesc(StatutDocument statut);

    /**
     * Documents accessibles sans restriction par employeId
     * (cas général : l'utilisateur n'a pas de fiche employé associée)
     */
    @Query("""
            SELECT DISTINCT d FROM Document d
            LEFT JOIN d.rolesLecture rl
            LEFT JOIN d.rolesEcriture re
            WHERE d.statut <> ong.fodde.modules.ged.entity.StatutDocument.ARCHIVE
            AND (
                d.createdBy = :userId
                OR rl IN :roles
                OR re IN :roles
            )
            ORDER BY d.createdAt DESC
            """)
    List<Document> findAccessibles(
            @Param("userId") UUID userId,
            @Param("roles") Collection<String> roles
    );

    /**
     * Documents accessibles en incluant les documents personnels de l'employé
     * (cas où l'utilisateur a une fiche employé associée : bulletins, contrats, évals)
     */
    @Query("""
            SELECT DISTINCT d FROM Document d
            LEFT JOIN d.rolesLecture rl
            LEFT JOIN d.rolesEcriture re
            WHERE d.statut <> ong.fodde.modules.ged.entity.StatutDocument.ARCHIVE
            AND (
                d.createdBy = :userId
                OR rl IN :roles
                OR re IN :roles
                OR d.concerneEmployeId = :employeId
            )
            ORDER BY d.createdAt DESC
            """)
    List<Document> findAccessiblesAvecEmploye(
            @Param("userId") UUID userId,
            @Param("roles") Collection<String> roles,
            @Param("employeId") UUID employeId
    );
}
