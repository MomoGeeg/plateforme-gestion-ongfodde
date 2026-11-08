package ong.fodde.modules.projets.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.dto.MontageDtos.PersonneResponse;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.historique.entity.TypeDemande;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.projets.dto.MontageDtos.Controle;
import ong.fodde.modules.projets.dto.ProjetRequest;
import ong.fodde.modules.projets.dto.ProjetResponse;
import ong.fodde.modules.projets.entity.Devise;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Fiche projet, équipe et cycle de vie.
 * Montage : le RP crée le projet en brouillon et désigne un chef de projet ; ensemble ils montent
 * le cadre logique et le budget ({@link MontageProjetService}). Le RP soumet ; le RAF vérifie le
 * budget ; le SE valide et le projet devient actif. Le RAF et le SE peuvent renvoyer en brouillon.
 */
@Service
@Transactional
public class ProjetService {

    /** Changements de statut après l'activation (suspension, clôture), par la route générique. */
    private static final Map<StatutProjet, Set<StatutProjet>> TRANSITIONS = Map.of(
        StatutProjet.ACTIF,      Set.of(StatutProjet.SUSPENDU, StatutProjet.EN_CLOTURE),
        StatutProjet.SUSPENDU,   Set.of(StatutProjet.ACTIF, StatutProjet.EN_CLOTURE),
        StatutProjet.EN_CLOTURE, Set.of(StatutProjet.CLOTURE),
        StatutProjet.CLOTURE,    Set.of(StatutProjet.ARCHIVE)
    );

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final MontageProjetService montageService;
    private final HistoriqueService historiqueService;
    private final DroitsProjet droits;
    private final ProjetMapper mapper;

    public ProjetService(ProjetRepository projetRepository, UserRepository userRepository,
                         MontageProjetService montageService, HistoriqueService historiqueService,
                         DroitsProjet droits, ProjetMapper mapper) {
        this.mapper = mapper;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
        this.montageService = montageService;
        this.historiqueService = historiqueService;
        this.droits = droits;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Fiche
    // ═════════════════════════════════════════════════════════════════════

    public ProjetResponse creerProjet(ProjetRequest request, User auteur) {
        verifierDates(request.dateDebut(), request.dateFin());
        String code = request.code() == null || request.code().isBlank() ? prochainCode() : request.code().trim();
        if (projetRepository.existsByCode(code)) {
            throw new BusinessException("Un projet avec le code '" + code + "' existe déjà");
        }

        Projet projet = new Projet();
        projet.setCode(code);
        remplir(projet, request);
        projet.setResponsableId(request.responsableId() != null ? request.responsableId() : auteur.getId());
        projet.setBudgetTotal(BigDecimal.ZERO);
        projet.setStatut(StatutProjet.BROUILLON);
        verifierCompte(projet.getResponsableId());
        if (request.equipeIds() != null) {
            request.equipeIds().forEach(id -> projet.getEquipe().add(compte(id)));
        }
        ajouterChefAEquipe(projet);

        Projet saved = projetRepository.save(projet);
        historiqueService.enregistrer(TypeDemande.PROJET, saved.getId(), null, StatutProjet.BROUILLON,
                auteur.getId(), null);
        return toResponse(saved);
    }

    /** Fiche modifiable en brouillon seulement, par le RP, le chef de projet ou l'admin. */
    public ProjetResponse modifierProjet(UUID id, ProjetRequest request, User user) {
        Projet projet = getOrThrow(id);
        droits.verifierMontage(projet, user);
        verifierDates(request.dateDebut(), request.dateFin());
        remplir(projet, request);
        if (request.responsableId() != null) {
            verifierCompte(request.responsableId());
            projet.setResponsableId(request.responsableId());
        }
        ajouterChefAEquipe(projet);
        return toResponse(projetRepository.save(projet));
    }

    private void remplir(Projet projet, ProjetRequest request) {
        projet.setTitre(request.titre().trim());
        projet.setDescription(request.description());
        projet.setObjectifGeneral(request.objectifGeneral());
        projet.setZoneGeographique(request.zoneGeographique());
        projet.setDateDebut(request.dateDebut());
        projet.setDateFin(request.dateFin());
        projet.setDevise(request.devise() != null ? request.devise() : Devise.XOF);
        if (request.chefProjetId() != null) {
            verifierCompte(request.chefProjetId());
        }
        projet.setChefProjetId(request.chefProjetId());
    }

    /** Le chef de projet fait partie de l'équipe. */
    private void ajouterChefAEquipe(Projet projet) {
        if (projet.getChefProjetId() != null) {
            projet.getEquipe().add(compte(projet.getChefProjetId()));
        }
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerProjets() {
        return projetRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Comptes actifs, triés par nom (choix du chef de projet, de l'équipe, des responsables). */
    @Transactional(readOnly = true)
    public List<PersonneResponse> personnes() {
        return userRepository.findAll().stream()
                .filter(u -> u.getStatut() == StatutUser.ACTIF)
                .sorted(java.util.Comparator.comparing((User u) -> u.getNom().toLowerCase())
                        .thenComparing(u -> u.getPrenom().toLowerCase()))
                .map(u -> new PersonneResponse(u.getId(), u.getPrenom() + " " + u.getNom(),
                        u.getRoles().stream().map(r -> r.getNom()).sorted().toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> listerMesProjets(UUID userId) {
        return projetRepository.findByResponsableOrMembre(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjetResponse obtenirProjet(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<EtapeHistorique> historique(UUID id) {
        Projet p = getOrThrow(id);
        EtapeHistorique creation = historiqueService.reconstituer(null, StatutProjet.BROUILLON,
                p.getResponsableId(), null, p.getCreatedAt());
        List<EtapeHistorique> anciennes = p.getStatut() == StatutProjet.BROUILLON ? List.of()
                : List.of(historiqueService.reconstituer(null, p.getStatut(), null, null, null));
        return historiqueService.historique(TypeDemande.PROJET, id, creation, anciennes);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Équipe
    // ═════════════════════════════════════════════════════════════════════

    public void ajouterMembreEquipe(UUID projetId, UUID userId, User user) {
        Projet projet = getOrThrow(projetId);
        verifierGestionEquipe(projet, user);
        User membre = compte(userId);
        if (projet.getEquipe().contains(membre)) {
            throw new BusinessException("Cette personne fait déjà partie de l'équipe");
        }
        projet.getEquipe().add(membre);
        projetRepository.save(projet);
    }

    public void retirerMembreEquipe(UUID projetId, UUID userId, User user) {
        Projet projet = getOrThrow(projetId);
        verifierGestionEquipe(projet, user);
        if (userId.equals(projet.getResponsableId()) || userId.equals(projet.getChefProjetId())) {
            throw new BusinessException("Le responsable et le chef de projet restent dans l'équipe : changez-les d'abord");
        }
        projet.getEquipe().removeIf(u -> u.getId().equals(userId));
        projetRepository.save(projet);
    }

    private void verifierGestionEquipe(Projet projet, User user) {
        if (!droits.estGestionnaire(projet, user)) {
            throw new AccessDeniedException("Seuls le RP et le chef de projet gèrent l'équipe");
        }
        if (Set.of(StatutProjet.CLOTURE, StatutProjet.ARCHIVE).contains(projet.getStatut())) {
            throw new BusinessException("Le projet est clôturé");
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Circuit de validation
    // ═════════════════════════════════════════════════════════════════════

    /** Le RP (ou le chef de projet) soumet le projet, une fois tous les contrôles passés. */
    public ProjetResponse soumettre(UUID id, User user) {
        Projet projet = getOrThrow(id);
        droits.verifierMontage(projet, user);
        List<String> manques = montageService.controles(projet).stream()
                .filter(c -> c.bloquant() && !c.ok())
                .map(Controle::libelle)
                .toList();
        if (!manques.isEmpty()) {
            throw new BusinessException("Avant de soumettre : " + String.join(" ; ", manques));
        }
        projet.setMotifRenvoi(null);
        return changer(projet, StatutProjet.EN_VALIDATION, user, null);
    }

    /** Le RAF a vérifié le budget : le projet va au SE. */
    public ProjetResponse verifierBudget(UUID id, User user) {
        Projet projet = getOrThrow(id);
        exigerStatut(projet, StatutProjet.EN_VALIDATION);
        return changer(projet, StatutProjet.EN_VALIDATION_SE, user, null);
    }

    /** Le SE valide : le projet devient actif. */
    public ProjetResponse valider(UUID id, User user) {
        Projet projet = getOrThrow(id);
        exigerStatut(projet, StatutProjet.EN_VALIDATION_SE);
        return changer(projet, StatutProjet.ACTIF, user, null);
    }

    /** Renvoi en brouillon, motif obligatoire : par le RAF à son étape, par le SE à la sienne. */
    public ProjetResponse renvoyer(UUID id, String motif, User user) {
        Projet projet = getOrThrow(id);
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Indiquez ce qu'il faut revoir");
        }
        boolean autorise = droits.aUnRole(user, "ROLE_ADMIN")
                || (projet.getStatut() == StatutProjet.EN_VALIDATION && droits.aUnRole(user, "ROLE_RAF"))
                || (projet.getStatut() == StatutProjet.EN_VALIDATION_SE && droits.aUnRole(user, "ROLE_SE"));
        if (projet.getStatut() != StatutProjet.EN_VALIDATION && projet.getStatut() != StatutProjet.EN_VALIDATION_SE) {
            throw new BusinessException("Seul un projet en validation peut être renvoyé");
        }
        if (!autorise) {
            throw new AccessDeniedException("Ce n'est pas à vous de décider à cette étape");
        }
        projet.setMotifRenvoi(motif.trim());
        return changer(projet, StatutProjet.BROUILLON, user, motif.trim());
    }

    /** Suspension, reprise et clôture d'un projet actif. */
    public ProjetResponse changerStatut(UUID id, StatutProjet nouveauStatut, User user, String commentaire) {
        Projet projet = getOrThrow(id);
        if (!TRANSITIONS.getOrDefault(projet.getStatut(), Set.of()).contains(nouveauStatut)) {
            throw new BusinessException("Changement impossible : " + projet.getStatut() + " → " + nouveauStatut);
        }
        return changer(projet, nouveauStatut, user, commentaire);
    }

    /** Prolongation d'un projet actif ou suspendu, justification obligatoire. */
    public ProjetResponse prolongerDateFin(UUID id, LocalDate nouvelleDateFin, String justification) {
        Projet projet = getOrThrow(id);
        if (projet.getStatut() != StatutProjet.ACTIF && projet.getStatut() != StatutProjet.SUSPENDU) {
            throw new BusinessException("La prolongation n'est possible que sur un projet actif ou suspendu");
        }
        if (nouvelleDateFin.isBefore(projet.getDateFin())) {
            throw new BusinessException("La nouvelle date de fin doit suivre l'actuelle : " + projet.getDateFin());
        }
        if (justification == null || justification.isBlank()) {
            throw new BusinessException("La justification est obligatoire pour prolonger un projet");
        }
        projet.setDateFin(nouvelleDateFin);
        return toResponse(projetRepository.save(projet));
    }

    // Alerte 30 jours avant la fin : à écrire avec les notifications.
    @Scheduled(cron = "0 0 7 * * *")
    public void verifierEcheancesProjets() {
        // volontairement vide
    }

    // ═════════════════════════════════════════════════════════════════════
    // Outils
    // ═════════════════════════════════════════════════════════════════════

    private ProjetResponse changer(Projet projet, StatutProjet apres, User user, String motif) {
        StatutProjet avant = projet.getStatut();
        projet.setStatut(apres);
        Projet saved = projetRepository.save(projet);
        historiqueService.enregistrer(TypeDemande.PROJET, saved.getId(), avant, apres, user.getId(), motif);
        return toResponse(saved);
    }

    private static void exigerStatut(Projet projet, StatutProjet attendu) {
        if (projet.getStatut() != attendu) {
            throw new BusinessException("Action impossible à cette étape du projet");
        }
    }

    private static void verifierDates(LocalDate debut, LocalDate fin) {
        if (fin.isBefore(debut)) {
            throw new BusinessException("La date de fin ne peut pas être antérieure à la date de début");
        }
    }

    /** PROJ-AAAA-NNN : premier numéro libre de l'année. */
    String prochainCode() {
        String prefixe = "PROJ-" + LocalDate.now().getYear() + "-";
        int n = 1;
        while (projetRepository.existsByCode(prefixe + String.format("%03d", n))) {
            n++;
        }
        return prefixe + String.format("%03d", n);
    }

    private User compte(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable : " + id));
    }

    private void verifierCompte(UUID id) {
        compte(id);
    }

    private Projet getOrThrow(UUID id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + id));
    }

    ProjetResponse toResponse(Projet p) {
        return mapper.toResponse(p);
    }
}
