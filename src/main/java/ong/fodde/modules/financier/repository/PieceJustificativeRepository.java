package ong.fodde.modules.financier.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.financier.entity.PieceJustificative;

@Repository
public interface PieceJustificativeRepository extends JpaRepository<PieceJustificative, UUID> {

    List<PieceJustificative> findByDepenseIdOrderByCreatedAtAsc(UUID depenseId);

    long countByDepenseId(UUID depenseId);
}
