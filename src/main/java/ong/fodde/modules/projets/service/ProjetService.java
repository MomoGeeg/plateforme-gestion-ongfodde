package ong.fodde.modules.projets.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.ProjetRequest;
import ong.fodde.modules.projets.dto.ProjetResponse;
import ong.fodde.modules.projets.entity.Devise;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    /** Transitions de statut autorisées */
    private static final Map<StatutProjet, Set<StatutProjet>> TRANSITIONS = Map.of(
        StatutProjet.BROUILLON,      Set.of(StatutProjet.EN_VALIDATION),
        StatutProjet.EN_VALIDATION,  Set.of(StatutProjet.ACTIF, StatutProjet.BROUILLON),
        StatutProjet.ACTIF,          Set.of(StatutProjet.SUSPENDU, StatutProjet.EN_CLOTURE),
        StatutProjet.SUSPENDU,       Set.of(StatutProjet.ACTIF, StatutProjet.EN_CLOTURE),
        StatutProjet.EN_CLOTURE,     Set.of(StatutProjet.CLOTURE),
        StatutProjet.CLOTURE,        Set.of(StatutProjet.ARCHIVE),
        StatutProjet.ARCHIVE,        Set.of()
    );

    public ProjetService(ProjetRepository projetRepository, UserRepository userRepository) {
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
    }

    public ProjetResponse creerProjet(ProjetRequest request) {
        if (projetRepository.existsByCode(request.code())) {
            throw new BusinessException("Un projet avec le code '" + request.code() + "' existe déjà");
        }

        Projet projet = new Projet();
        projet.setCode(request.code());
        projet.setTitre(request.titre());
        projet.setDescription(request.description());
        projet.setObjectifGeneral(request.objectifGeneral());
        projet.setZoneGeographique(request.zoneGeographique());
        projet.setDateDebut(request.dateDebut());
        projet.setDateFin(request.dateFin());
        projet.setBudgetTotal(request.budgetTotal());
        projet.setDevise(request.devise() != null ? request.devise() : Devise.XOF);
        projet.setStatut(StatutProjet.BROUILLON);
        projet.setResponsableId(request.responsableId());

        Projet saved = projetRepository.save(projet);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerProjets() {
        return projetRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjetResponse obtenirProjet(UUID id) {
        Projet projet = projetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + id));
        return toResponse(projet);
    }

    public ProjetResponse changerStatut(UUID id, StatutProjet nouveauStatut) {
        Projet projet = projetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + id));

        StatutProjet statutActuel = projet.getStatut();
        Set<StatutProjet> statutsAutorisés = TRANSITIONS.getOrDefault(statutActuel, Set.of());

        if (!statutsAutorisés.contains(nouveauStatut)) {
            throw new BusinessException(
                "Transition de statut invalide : " + statutActuel + " → " + nouveauStatut
            );
        }

        projet.setStatut(nouveauStatut);
        Projet saved = projetRepository.save(projet);
        return toResponse(saved);
    }

    public void ajouterMembreEquipe(UUID projetId, UUID userId) {
        Projet projet = projetRepository.findById(projetId)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + projetId));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant : " + userId));

        projet.getEquipe().add(user);
        projetRepository.save(projet);
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerMesProjets(UUID userId) {
        return projetRepository.findByResponsableId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    private ProjetResponse toResponse(Projet projet) {
        return new ProjetResponse(
            projet.getId(),
            projet.getCode(),
            projet.getTitre(),
            projet.getDescription(),
            projet.getObjectifGeneral(),
            projet.getZoneGeographique(),
            projet.getDateDebut(),
            projet.getDateFin(),
            projet.getBudgetTotal(),
            projet.getDevise(),
            projet.getStatut(),
            projet.getResponsableId(),
            projet.getCreatedAt()
        );
    }
}
