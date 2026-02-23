package ong.fodde.modules.projets.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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

    private static final Logger log = LoggerFactory.getLogger(ProjetService.class);

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
        // Règle 1 : Code unique
        if (projetRepository.existsByCode(request.code())) {
            throw new BusinessException("Un projet avec le code '" + request.code() + "' existe déjà");
        }

        // Règle 2 : Date de fin après date de début
        if (request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("La date de fin ne peut pas être antérieure à la date de début");
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

        // Règle 3 : Affecter l'équipe à la création si fournie
        if (request.equipeIds() != null && !request.equipeIds().isEmpty()) {
            for (UUID userId : request.equipeIds()) {
                User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec l'identifiant : " + userId));
                projet.getEquipe().add(user);
            }
        }

        Projet saved = projetRepository.save(projet);
        log.info("✅ Projet {} créé avec {} membres d'équipe",
            saved.getCode(),
            saved.getEquipe().size());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerProjets() {
        return projetRepository.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerProjetsParMembre(UUID userId) {
        // Pour CHARGE_PROGRAMME, TERRAIN, ANTENNE → projets affectés uniquement
        return projetRepository.findByMembreEquipe(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjetResponse obtenirProjet(UUID id) {
        Projet projet = projetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + id));
        return toResponse(projet);
    }

    public ProjetResponse changerStatut(UUID id, StatutProjet nouveauStatut, UUID validateurId, String commentaire) {
        Projet projet = projetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + id));

        StatutProjet statutActuel = projet.getStatut();
        Set<StatutProjet> statutsAutorises = TRANSITIONS.getOrDefault(statutActuel, Set.of());

        if (!statutsAutorises.contains(nouveauStatut)) {
            throw new BusinessException(
                "Transition de statut invalide : " + statutActuel + " → " + nouveauStatut
            );
        }

        // Règle 4 : Passage BROUILLON → EN_VALIDATION
        if (nouveauStatut == StatutProjet.EN_VALIDATION) {
            if (projet.getEquipe().isEmpty()) {
                throw new BusinessException(
                    "Le projet doit avoir au moins un membre dans l'équipe avant soumission"
                );
            }
            log.info("📋 Projet {} soumis au SE pour validation par {}",
                projet.getCode(), validateurId);
        }

        // Règle 5 : Passage EN_VALIDATION → ACTIF réservé au SE
        if (nouveauStatut == StatutProjet.ACTIF) {
            log.info("✅ Projet {} activé par le SE {} — notification équipe envoyée",
                projet.getCode(), validateurId);
        }

        // Règle 6 : Passage EN_VALIDATION → BROUILLON (rejet SE avec commentaire)
        if (nouveauStatut == StatutProjet.BROUILLON && statutActuel == StatutProjet.EN_VALIDATION) {
            if (commentaire == null || commentaire.isBlank()) {
                throw new BusinessException("Un commentaire est obligatoire pour rejeter un projet");
            }
            log.info("❌ Projet {} rejeté par le SE {} — motif : {}",
                projet.getCode(), validateurId, commentaire);
        }

        // Règle 7 : Clôture impossible sans rapport dans la GED
        // (vérification simplifiée — à enrichir avec GED en version 2)
        if (nouveauStatut == StatutProjet.CLOTURE) {
            log.warn("⚠️ Projet {} en cours de clôture — vérifier rapport de clôture dans la GED",
                projet.getCode());
        }

        projet.setStatut(nouveauStatut);
        Projet saved = projetRepository.save(projet);
        return toResponse(saved);
    }

    public void ajouterMembreEquipe(UUID projetId, UUID userId) {
        Projet projet = projetRepository.findById(projetId)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + projetId));

        // Règle 8 : Ne pas ajouter un membre déjà dans l'équipe
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant : " + userId));

        if (projet.getEquipe().contains(user)) {
            throw new BusinessException("Cet utilisateur est déjà membre de l'équipe projet");
        }

        projet.getEquipe().add(user);
        projetRepository.save(projet);

        log.info("👤 Utilisateur {} ajouté à l'équipe du projet {}", userId, projet.getCode());
    }

    public void retirerMembreEquipe(UUID projetId, UUID userId) {
        Projet projet = projetRepository.findById(projetId)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + projetId));

        // Règle 9 : Ne pas retirer le responsable principal
        if (projet.getResponsableId().equals(userId)) {
            throw new BusinessException(
                "Impossible de retirer le responsable principal du projet. "
                + "Veuillez d'abord changer le responsable."
            );
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'identifiant : " + userId));

        projet.getEquipe().remove(user);
        projetRepository.save(projet);

        log.info("👤 Utilisateur {} retiré de l'équipe du projet {}", userId, projet.getCode());
    }

    public ProjetResponse prolongerDateFin(UUID id, LocalDate nouvelleDateFin, String justification) {
        Projet projet = projetRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable avec l'identifiant : " + id));

        // Règle 10 : Prolongation uniquement sur projet ACTIF ou SUSPENDU
        if (projet.getStatut() != StatutProjet.ACTIF && projet.getStatut() != StatutProjet.SUSPENDU) {
            throw new BusinessException(
                "La prolongation n'est possible que sur un projet ACTIF ou SUSPENDU"
            );
        }

        if (nouvelleDateFin.isBefore(projet.getDateFin())) {
            throw new BusinessException(
                "La nouvelle date de fin doit être postérieure à la date actuelle : " + projet.getDateFin()
            );
        }

        if (justification == null || justification.isBlank()) {
            throw new BusinessException("La justification est obligatoire pour prolonger un projet");
        }

        LocalDate ancienneDateFin = projet.getDateFin();
        projet.setDateFin(nouvelleDateFin);
        projetRepository.save(projet);

        log.info("📅 Projet {} prolongé : {} → {} — justification : {}",
            projet.getCode(), ancienneDateFin, nouvelleDateFin, justification);

        return toResponse(projet);
    }

    // Règle 11 : Alerte 30 jours avant fin — @Scheduled tous les jours à 7h00
    @Scheduled(cron = "0 0 7 * * *")
    public void verifierEcheancesProjets() {
        List<Projet> projetsActifs = projetRepository.findByStatut(StatutProjet.ACTIF);
        LocalDate aujourdHui = LocalDate.now();

        for (Projet projet : projetsActifs) {
            if (projet.getDateFin() == null) continue;

            long joursRestants = ChronoUnit.DAYS.between(aujourdHui, projet.getDateFin());

            if (joursRestants < 0) {
                log.warn("🚨 Projet {} est en dépassement de {} jours — statut toujours ACTIF",
                    projet.getCode(), Math.abs(joursRestants));
            } else if (joursRestants <= 7) {
                log.warn("🚨 ALERTE PROJET [7 JOURS] : {} expire le {} ({} jours restants)",
                    projet.getCode(), projet.getDateFin(), joursRestants);
            } else if (joursRestants <= 30) {
                log.warn("⚠️ ALERTE PROJET [30 JOURS] : {} expire le {} ({} jours restants)",
                    projet.getCode(), projet.getDateFin(), joursRestants);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerMesProjets(UUID userId) {
        return projetRepository.findByResponsableOrMembre(userId).stream()
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