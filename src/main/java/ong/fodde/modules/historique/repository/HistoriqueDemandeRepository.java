package ong.fodde.modules.historique.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.historique.entity.HistoriqueDemande;
import ong.fodde.modules.historique.entity.TypeDemande;

@Repository
public interface HistoriqueDemandeRepository extends JpaRepository<HistoriqueDemande, UUID> {

    List<HistoriqueDemande> findByTypeDemandeAndDemandeIdOrderByDateActionAsc(TypeDemande typeDemande, UUID demandeId);
}
