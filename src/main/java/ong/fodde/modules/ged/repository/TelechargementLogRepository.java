package ong.fodde.modules.ged.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.ged.entity.TelechargementLog;

@Repository
public interface TelechargementLogRepository extends JpaRepository<TelechargementLog, UUID> {

    List<TelechargementLog> findByDocumentId(UUID documentId);

    List<TelechargementLog> findByUserId(UUID userId);
}
