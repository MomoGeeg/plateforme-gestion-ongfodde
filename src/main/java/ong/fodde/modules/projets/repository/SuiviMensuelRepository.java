package ong.fodde.modules.projets.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ong.fodde.modules.projets.entity.StatutSuivi;
import ong.fodde.modules.projets.entity.SuiviMensuel;
import ong.fodde.modules.projets.entity.TypeSuivi;

@Repository
public interface SuiviMensuelRepository extends JpaRepository<SuiviMensuel, UUID> {

    Optional<SuiviMensuel> findByTechnicienIdAndTypeAndMoisAndIsDeletedFalse(UUID technicienId, TypeSuivi type, String mois);

    List<SuiviMensuel> findByTechnicienIdAndIsDeletedFalseOrderByMoisDescTypeAsc(UUID technicienId);

    List<SuiviMensuel> findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(Collection<UUID> technicienIds);

    List<SuiviMensuel> findByTechnicienIdInAndStatutAndIsDeletedFalseOrderByCreatedAtAsc(Collection<UUID> technicienIds,
                                                                                         StatutSuivi statut);

    List<SuiviMensuel> findByStatutAndIsDeletedFalseOrderByDateDecisionDesc(StatutSuivi statut);
}
