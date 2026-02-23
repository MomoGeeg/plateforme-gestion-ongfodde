package ong.fodde.modules.ged.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.ged.entity.Document;
import ong.fodde.modules.ged.entity.StatutDocument;
import ong.fodde.modules.ged.entity.TypeDocument;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByProjetId(UUID projetId);

    List<Document> findByStatut(StatutDocument statut);

    List<Document> findByTypeDocument(TypeDocument type);

    List<Document> findByAccessibleCaTrue();
}
