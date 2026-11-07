package ong.fodde.modules.missions.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.historique.entity.TypeDemande;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.shared.securite.AccesPersonnel;
import ong.fodde.modules.missions.dto.MissionRequest;
import ong.fodde.modules.missions.dto.MissionResponse;
import ong.fodde.modules.missions.entity.Mission;
import ong.fodde.modules.missions.entity.StatutMission;
import ong.fodde.modules.missions.repository.MissionRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

import static ong.fodde.shared.util.Prechargement.precharger;

@Service
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final HistoriqueService historiqueService;
    private final AccesPersonnel accesPersonnel;

    public MissionService(MissionRepository missionRepository,
                          UserRepository userRepository,
                          ProjetRepository projetRepository,
                          HistoriqueService historiqueService,
                          AccesPersonnel accesPersonnel) {
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.projetRepository = projetRepository;
        this.historiqueService = historiqueService;
        this.accesPersonnel = accesPersonnel;
    }

    /**
     * Historique de la mission. Visible par l'employé concerné, la personne qui l'a
     * saisie, et les rôles qui voient toutes les missions.
     */
    @Transactional(readOnly = true)
    public List<EtapeHistorique> historique(UUID id, User user) {
        Mission m = getOrThrow(id);
        if (!user.getId().equals(m.getCreatedBy())) {
            accesPersonnel.verifierUtilisateur(m.getEmployeId(), user,
                    "ROLE_RAF", "ROLE_SE", "ROLE_ADMIN", "ROLE_RESPONSABLE_PROGRAMMES");
        }
        // Missions traitées avant l'historique : seul le rejet (avec son motif) est connu.
        List<EtapeHistorique> anciennes = m.getStatut() == StatutMission.REJETEE
                ? List.of(historiqueService.reconstituer(null, StatutMission.REJETEE, null, m.getMotifRejet(), null))
                : m.getStatut() == StatutMission.SOUMISE ? List.of()
                : List.of(historiqueService.reconstituer(null, m.getStatut(), null, null, null));
        EtapeHistorique creation = historiqueService.reconstituer(null, StatutMission.SOUMISE,
                m.getCreatedBy(), null, m.getCreatedAt());
        return historiqueService.historique(TypeDemande.MISSION, id, creation, anciennes);
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

        Mission saved = missionRepository.save(mission);
        historiqueService.enregistrer(TypeDemande.MISSION, saved.getId(), null, StatutMission.SOUMISE, createdBy, null);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listerToutesMissions() {
        return toResponses(missionRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> mesMissions(UUID userId) {
        return toResponses(missionRepository.findByEmployeId(userId));
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> listerMissionsParProjet(UUID projetId) {
        return toResponses(missionRepository.findByProjetId(projetId));
    }

    /** Conversion d'une liste : employés et projets chargés en une requête chacun. */
    private List<MissionResponse> toResponses(List<Mission> missions) {
        precharger(userRepository, missions, Mission::getEmployeId);
        precharger(projetRepository, missions, Mission::getProjetId);
        return missions.stream().map(this::toResponse).toList();
    }

    /** N+1 approves the mission request */
    public MissionResponse approuverN1(UUID id, UUID userId) {
        Mission mission = getOrThrow(id);
        if (mission.getStatut() != StatutMission.SOUMISE) {
            throw new BusinessException("Seule une mission SOUMISE peut être approuvée N+1 (statut : " + mission.getStatut() + ")");
        }
        mission.setStatut(StatutMission.APPROUVEE_N1);
        Mission saved = missionRepository.save(mission);
        historiqueService.enregistrer(TypeDemande.MISSION, id, StatutMission.SOUMISE, StatutMission.APPROUVEE_N1, userId, null);
        return toResponse(saved);
    }

    /** RAF validates the mission (from SOUMISE or APPROUVEE_N1) */
    public MissionResponse validerMission(UUID id, UUID userId) {
        Mission mission = getOrThrow(id);
        if (mission.getStatut() != StatutMission.SOUMISE && mission.getStatut() != StatutMission.APPROUVEE_N1) {
            throw new BusinessException("Seule une mission SOUMISE ou APPROUVEE_N1 peut être validée (statut : " + mission.getStatut() + ")");
        }
        StatutMission avant = mission.getStatut();
        mission.setStatut(StatutMission.VALIDEE_RAF);
        Mission saved = missionRepository.save(mission);
        historiqueService.enregistrer(TypeDemande.MISSION, id, avant, StatutMission.VALIDEE_RAF, userId, null);
        return toResponse(saved);
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
        StatutMission avant = mission.getStatut();
        mission.setStatut(StatutMission.REJETEE);
        mission.setMotifRejet(motif);
        Mission saved = missionRepository.save(mission);
        historiqueService.enregistrer(TypeDemande.MISSION, id, avant, StatutMission.REJETEE, userId, motif);
        return toResponse(saved);
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
