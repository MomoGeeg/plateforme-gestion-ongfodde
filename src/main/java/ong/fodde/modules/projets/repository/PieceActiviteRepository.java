package ong.fodde.modules.projets.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.PieceActivite;
import ong.fodde.modules.projets.entity.TypePiece;

@Repository
public interface PieceActiviteRepository extends JpaRepository<PieceActivite, UUID> {

    List<PieceActivite> findByActiviteIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID activiteId);

    List<PieceActivite> findByActiviteIdAndTypeAndIsDeletedFalse(UUID activiteId, TypePiece type);

    long countByActiviteIdAndTypeAndIsDeletedFalse(UUID activiteId, TypePiece type);
}
