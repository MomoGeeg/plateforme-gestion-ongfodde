package ong.fodde.modules.ged.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.ged.entity.VersionDocument;

@Repository
public interface VersionDocumentRepository extends JpaRepository<VersionDocument, UUID> {

    List<VersionDocument> findByDocumentId(UUID documentId);

    List<VersionDocument> findByDocumentIdOrderByVersionDesc(UUID documentId);
}
