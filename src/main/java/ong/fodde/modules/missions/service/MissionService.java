package ong.fodde.modules.missions.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.missions.dto.MissionRequest;
import ong.fodde.modules.missions.dto.MissionResponse;
import ong.fodde.modules.missions.entity.Mission;
import ong.fodde.modules.missions.entity.StatutMission;
import ong.fodde.modules.missions.repository.MissionRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;

    public MissionService(MissionRepository missionRepository,
                          UserRepository userRepository,
                          ProjetRepository projetRepository) {
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.projetRepository = projetRepository;
    }

    public MissionResponse soumettreDemandesMission(MissionRequest request, UUID createdBy) {
        Mission mission = new Mission();
        mission.setEmployeId(request.employeId());
        mission.setProjetId(request.projetId());
        mission.setDestination(request.destination());
        mission.setDateDebut(request.dateDebut());
        mission.setDateFin(request.dateFin());
        mission.setMotif(request.motif());
        mission.setBudgetEstime(request.budgetEstime());
        mission.setStatut(StatutMission.SOUMISE);
        mission.setCreatedBy(createdBy);

        return toResponse(missionRepository.save(mission));
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listerToutesMissions() {
        return missionRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listerMissionsParProjet(UUID projetId) {
        return missionRepository.findByProjetId(projetId).stream().map(this::toResponse).toList();
    }

    /** N+1 approves the mission request */
    public MissionResponse approuverN1(UUID id) {
        Mission mission = getOrThrow(id);
        if (mission.getStatut() != StatutMission.SOUMISE) {
            throw new BusinessException("Seule une mission SOUMISE peut être approuvée N+1 (statut : " + mission.getStatut() + ")");
        }
        mission.setStatut(StatutMission.APPROUVEE_N1);
        return toResponse(missionRepository.save(mission));
    }

    /** RAF validates the mission (from SOUMISE or APPROUVEE_N1) */
    public MissionResponse validerMission(UUID id, UUID userId) {
        Mission mission = getOrThrow(id);
        if (mission.getStatut() != StatutMission.SOUMISE && mission.getStatut() != StatutMission.APPROUVEE_N1) {
            throw new BusinessException("Seule une mission SOUMISE ou APPROUVEE_N1 peut être validée (statut : " + mission.getStatut() + ")");
        }
        mission.setStatut(StatutMission.VALIDEE_RAF);
        return toResponse(missionRepository.save(mission));
    }

    /** RAF or N+1 rejects the mission */
    public MissionResponse rejeterMission(UUID id, String motif, UUID userId) {
        Mission mission = getOrThrow(id);
        if (mission.getStatut() == StatutMission.VALIDEE_RAF || mission.getStatut() == StatutMission.REJETEE) {
            throw new BusinessException("Impossible de rejeter une mission déjà " + mission.getStatut());
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        mission.setStatut(StatutMission.REJETEE);
        mission.setMotifRejet(motif);
        return toResponse(missionRepository.save(mission));
    }

    private Mission getOrThrow(UUID id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
    }

    private MissionResponse toResponse(Mission m) {
        String employeNom = userRepository.findById(m.getEmployeId())
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse(null);

        String projetCode = projetRepository.findById(m.getProjetId())
                .map(p -> p.getCode())
                .orElse(null);

        return new MissionResponse(
                m.getId(),
                m.getEmployeId(),
                employeNom,
                m.getProjetId(),
                projetCode,
                m.getDestination(),
                m.getDateDebut(),
                m.getDateFin(),
                m.getMotif(),
                m.getBudgetEstime(),
                m.getStatut(),
                m.getMotifRejet(),
                m.getCreatedAt()
        );
    }
}
