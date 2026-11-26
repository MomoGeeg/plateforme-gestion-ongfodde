package ong.fodde.modules.projets.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.ged.service.StockageFichierService;
import ong.fodde.modules.projets.dto.SuiviDtos.ActiviteDuMois;
import ong.fodde.modules.projets.dto.SuiviDtos.DecisionRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.MembreEquipe;
import ong.fodde.modules.projets.dto.SuiviDtos.ProjetDuSuivi;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.entity.StatutActivite;
import ong.fodde.modules.projets.entity.StatutSuivi;
import ong.fodde.modules.projets.entity.SuiviMensuel;
import ong.fodde.modules.projets.entity.TypeActivite;
import ong.fodde.modules.projets.entity.TypeSuivi;
import ong.fodde.modules.projets.entity.AnimateurProjet;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.AnimateurProjetRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.SuiviMensuelRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Suivi mensuel du terrain. Le technicien (ou l'animateur) remet, pour un de ses projets, son
 * planning (texte) et son rapport (fichier Word) du mois. Le superviseur de ses périmètres de ce
 * projet le vise, en déposant au besoin une version corrigée, et le transmet au coordonnateur du
 * projet, qui le valide ou le renvoie à son auteur. Validé, le document est visible du RAF, du RP,
 * du SE et du suivi-évaluation. Chaque projet a un coordonnateur ; chaque périmètre, un seul projet.
 */
@Service
@Transactional
public class SuiviTerrainService {

    /** Fichier d'un rapport, pour le téléchargement. */
    public record Fichier(String nom, String contentType, byte[] contenu) {
    }

    static final long TAILLE_FICHIER_MAX = 10L * 1024 * 1024;
    private static final Map<String, String> TYPES_WORD = Map.of(
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    /** Lecture des documents validés. */
    private static final String[] ROLES_DIFFUSION = {
            "ROLE_RAF", "ROLE_RESPONSABLE_PROGRAMMES", "ROLE_SE", "ROLE_RESPONSABLE_SE"};

    private final SuiviMensuelRepository suiviRepository;
    private final PerimetreRepository perimetreRepository;
    private final ActiviteRepository activiteRepository;
    private final UserRepository userRepository;
    private final AnimateurProjetRepository animateurRepository;
    private final ProjetRepository projetRepository;
    private final DroitsProjet droits;
    private final StockageFichierService stockage;

    public SuiviTerrainService(SuiviMensuelRepository suiviRepository, PerimetreRepository perimetreRepository,
                               ActiviteRepository activiteRepository, UserRepository userRepository,
                               AnimateurProjetRepository animateurRepository, ProjetRepository projetRepository,
                               DroitsProjet droits, StockageFichierService stockage) {
        this.suiviRepository = suiviRepository;
        this.perimetreRepository = perimetreRepository;
        this.activiteRepository = activiteRepository;
        this.userRepository = userRepository;
        this.animateurRepository = animateurRepository;
        this.projetRepository = projetRepository;
        this.droits = droits;
        this.stockage = stockage;
    }

    /** Projets pour lesquels l'utilisateur remet un planning ou un rapport. */
    @Transactional(readOnly = true)
    public List<ProjetDuSuivi> mesProjets(User user) {
        return projetsDe(user.getId()).stream()
                .map(p -> new ProjetDuSuivi(p.getId(), p.getCode(), p.getTitre())).toList();
    }

    /**
     * Remise du planning ou du rapport d'un mois pour un projet. Le rapport est un fichier Word.
     * Une nouvelle remise remplace la précédente tant qu'elle n'est pas visée.
     */
    public SuiviResponse soumettre(SuiviRequest request, MultipartFile fichier, User user) {
        List<Projet> projets = projetsDe(user.getId());
        if (projets.isEmpty()) {
            throw new BusinessException("Aucun périmètre ni projet ne vous est affecté");
        }
        Projet projet = request.projetId() == null && projets.size() == 1
                ? projets.get(0)
                : projets.stream().filter(p -> p.getId().equals(request.projetId())).findFirst()
                        .orElseThrow(() -> new BusinessException("Choisissez un de vos projets"));
        if (projet.getChefProjetId() == null) {
            throw new BusinessException("Le projet " + projet.getCode() + " n'a pas de coordonnateur");
        }
        YearMonth mois = YearMonth.parse(request.mois());
        if (request.type() == TypeSuivi.RAPPORT && mois.isAfter(YearMonth.now())) {
            throw new BusinessException("Le rapport d'un mois à venir ne peut pas encore être remis");
        }
        String contenu = request.contenu() == null || request.contenu().isBlank() ? null : request.contenu().trim();
        boolean avecFichier = fichier != null && !fichier.isEmpty();
        if (request.type() == TypeSuivi.RAPPORT && !avecFichier) {
            throw new BusinessException("Joignez le rapport au format Word");
        }
        if (request.type() == TypeSuivi.PLANNING && contenu == null) {
            throw new BusinessException("Décrivez les activités prévues");
        }
        String extension = avecFichier ? verifierWord(fichier) : null;
        SuiviMensuel suivi = suiviRepository
                .findByTechnicienIdAndTypeAndMoisAndProjetIdAndIsDeletedFalse(user.getId(), request.type(),
                        request.mois(), projet.getId())
                .orElseGet(SuiviMensuel::new);
        if (suivi.getId() != null && suivi.getStatut() == StatutSuivi.TRANSMIS) {
            throw new BusinessException("Ce document est déjà validé par le superviseur et transmis au coordonnateur");
        }
        if (suivi.getId() != null && suivi.getStatut() == StatutSuivi.VALIDE) {
            throw new BusinessException("Ce document est déjà validé");
        }
        suivi.setType(request.type());
        suivi.setTechnicienId(user.getId());
        suivi.setProjetId(projet.getId());
        suivi.setMois(request.mois());
        suivi.setContenu(contenu);
        suivi.setContenuInitial(null);
        suivi.setStatut(StatutSuivi.SOUMIS);
        suivi.setCommentaire(null);
        suivi.setDecidePar(null);
        suivi.setDateDecision(null);
        suivi.setVisePar(null);
        suivi.setDateVisa(null);
        suivi.setFichierInitialNom(null);
        suivi.setFichierInitialChemin(null);
        if (avecFichier) {
            suivi = suiviRepository.save(suivi);
            enregistrerFichier(suivi, fichier, extension);
        } else {
            suivi.setFichierNom(null);
            suivi.setFichierChemin(null);
            suivi.setFichierTaille(null);
        }
        return toResponse(suiviRepository.save(suivi), user);
    }

    /**
     * Version corrigée du rapport, déposée par le superviseur avant son visa. Le fichier de
     * l'auteur est gardé la première fois.
     */
    public SuiviResponse deposerCorrection(UUID id, MultipartFile fichier, User user) {
        SuiviMensuel suivi = suiviExistant(id);
        if (suivi.getStatut() != StatutSuivi.SOUMIS || !doitDecider(user, suivi)) {
            throw new AccessDeniedException("Seul le superviseur corrige un document remis, avant son visa");
        }
        if (fichier == null || fichier.isEmpty()) {
            throw new BusinessException("Le fichier est vide");
        }
        String extension = verifierWord(fichier);
        if (suivi.getFichierInitialChemin() == null) {
            suivi.setFichierInitialNom(suivi.getFichierNom());
            suivi.setFichierInitialChemin(suivi.getFichierChemin());
        }
        enregistrerFichier(suivi, fichier, extension);
        return toResponse(suiviRepository.save(suivi), user);
    }

    /** Fichier du document (ou la version de l'auteur, avant correction). */
    @Transactional(readOnly = true)
    public Fichier telecharger(UUID id, boolean initial, User user) {
        SuiviMensuel suivi = suiviExistant(id);
        if (!peutVoir(user, suivi)) {
            throw new AccessDeniedException("Document réservé à son circuit de validation");
        }
        String chemin = initial ? suivi.getFichierInitialChemin() : suivi.getFichierChemin();
        String nom = initial ? suivi.getFichierInitialNom() : suivi.getFichierNom();
        if (chemin == null) {
            throw new ResourceNotFoundException("Aucun fichier pour ce document");
        }
        return new Fichier(nom, TYPES_WORD.getOrDefault(extension(chemin), "application/octet-stream"),
                stockage.lire(chemin));
    }

    /** Plannings et rapports remis par le technicien connecté. */
    @Transactional(readOnly = true)
    public List<SuiviResponse> mesSuivis(User user) {
        return suiviRepository.findByTechnicienIdAndIsDeletedFalseOrderByMoisDescTypeAsc(user.getId()).stream()
                .map(s -> toResponse(s, user)).toList();
    }

    /**
     * Plannings et rapports suivis par l'utilisateur connecté : ceux des périmètres qu'il supervise
     * (superviseur) et ceux de ses projets (coordonnateur).
     */
    @Transactional(readOnly = true)
    public List<SuiviResponse> suivisEquipe(User user) {
        Set<UUID> techniciens = new java.util.HashSet<>(techniciensEncadres(user.getId()).keySet());
        techniciens.addAll(animateursEncadres(user.getId()).keySet());
        techniciens.addAll(techniciensDesProjets(user.getId()));
        if (techniciens.isEmpty()) {
            return List.of();
        }
        return suiviRepository.findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(techniciens).stream()
                .filter(s -> estDuCircuit(user, s))
                .map(s -> toResponse(s, user)).toList();
    }

    /** Plannings et rapports validés par le coordonnateur : RAF, RP, SE et suivi-évaluation. */
    @Transactional(readOnly = true)
    public List<SuiviResponse> valides(User user) {
        return suiviRepository.findByStatutAndIsDeletedFalseOrderByDateDecisionDesc(StatutSuivi.VALIDE).stream()
                .map(s -> toResponse(s, user)).toList();
    }

    /**
     * Décision sur un document. Remis (SOUMIS) : le superviseur le vise, en corrigeant au besoin le
     * texte (planning) ou le fichier (rapport, par {@link #deposerCorrection}), et il passe chez le
     * coordonnateur du projet ; sans superviseur, le coordonnateur décide directement. Transmis : le
     * coordonnateur valide, et le document est diffusé. Un renvoi à l'auteur exige un commentaire.
     */
    public SuiviResponse decider(UUID id, DecisionRequest request, User user) {
        SuiviMensuel suivi = suiviExistant(id);
        if (suivi.getStatut() != StatutSuivi.SOUMIS && suivi.getStatut() != StatutSuivi.TRANSMIS) {
            throw new BusinessException("Ce document a déjà été traité");
        }
        if (!doitDecider(user, suivi)) {
            throw new AccessDeniedException(suivi.getStatut() == StatutSuivi.TRANSMIS
                    ? "Seul le coordonnateur du projet valide ce document"
                    : "Seul le superviseur valide ce document");
        }
        boolean valide = Boolean.TRUE.equals(request.valide());
        String commentaire = request.commentaire() == null || request.commentaire().isBlank()
                ? null : request.commentaire().trim();
        LocalDateTime maintenant = LocalDateTime.now();
        if (!valide) {
            if (commentaire == null) {
                throw new BusinessException("Indiquez ce qui est à corriger");
            }
            suivi.setStatut(StatutSuivi.A_REVOIR);
            suivi.setCommentaire(commentaire);
            suivi.setDecidePar(user.getId());
            suivi.setDateDecision(maintenant);
            return toResponse(suiviRepository.save(suivi), user);
        }
        suivi.setCommentaire(commentaire);
        if (suivi.getStatut() == StatutSuivi.SOUMIS) {
            corriger(suivi, request.contenu());
            if (!user.getId().equals(coordonnateurDe(suivi))) {
                suivi.setStatut(StatutSuivi.TRANSMIS);
                suivi.setVisePar(user.getId());
                suivi.setDateVisa(maintenant);
                return toResponse(suiviRepository.save(suivi), user);
            }
        }
        suivi.setStatut(StatutSuivi.VALIDE);
        suivi.setDecidePar(user.getId());
        suivi.setDateDecision(maintenant);
        return toResponse(suiviRepository.save(suivi), user);
    }

    /** Texte corrigé par le superviseur : le texte de l'auteur est gardé la première fois. */
    private static void corriger(SuiviMensuel suivi, String contenu) {
        if (contenu == null || contenu.isBlank() || contenu.trim().equals(suivi.getContenu())) {
            return;
        }
        if (suivi.getContenuInitial() == null) {
            suivi.setContenuInitial(suivi.getContenu());
        }
        suivi.setContenu(contenu.trim());
    }

    /**
     * Vrai si c'est à l'utilisateur de décider maintenant : un document remis attend un superviseur
     * des périmètres de l'auteur dans ce projet (à défaut, le coordonnateur du projet) ; un document
     * transmis attend le coordonnateur. L'auteur ne décide jamais de son propre document.
     */
    private boolean doitDecider(User user, SuiviMensuel s) {
        if (user == null || user.getId().equals(s.getTechnicienId())) {
            return false;
        }
        if (s.getStatut() != StatutSuivi.SOUMIS && s.getStatut() != StatutSuivi.TRANSMIS) {
            return false;
        }
        if (droits.aUnRole(user, "ROLE_ADMIN")) {
            return true;
        }
        if (s.getStatut() == StatutSuivi.TRANSMIS) {
            return user.getId().equals(coordonnateurDe(s));
        }
        Set<UUID> superviseurs = superviseursDe(s);
        return superviseurs.isEmpty() ? user.getId().equals(coordonnateurDe(s)) : superviseurs.contains(user.getId());
    }

    /** Superviseur des périmètres de l'auteur dans ce projet, ou coordonnateur du projet. */
    private boolean estDuCircuit(User user, SuiviMensuel s) {
        return user.getId().equals(coordonnateurDe(s)) || superviseursDe(s).contains(user.getId());
    }

    /** L'auteur, son circuit de validation, l'admin ; une fois validé, le RAF, le RP, le SE et le S&E. */
    private boolean peutVoir(User user, SuiviMensuel s) {
        return user.getId().equals(s.getTechnicienId()) || estDuCircuit(user, s)
                || droits.aUnRole(user, "ROLE_ADMIN")
                || (s.getStatut() == StatutSuivi.VALIDE && droits.aUnRole(user, ROLES_DIFFUSION));
    }

    private Set<UUID> superviseursDe(SuiviMensuel s) {
        return perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(s.getTechnicienId()).stream()
                .filter(p -> Objects.equals(p.getProjetId(), s.getProjetId()))
                .map(Perimetre::getSuperviseurId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private UUID coordonnateurDe(SuiviMensuel s) {
        return s.getProjetId() == null ? null
                : projetRepository.findById(s.getProjetId()).map(Projet::getChefProjetId).orElse(null);
    }

    /** Projets de l'auteur : ceux de ses périmètres et ceux qu'il anime. */
    private List<Projet> projetsDe(UUID auteurId) {
        Map<UUID, Projet> projets = new LinkedHashMap<>();
        perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(auteurId).stream()
                .map(Perimetre::getProjetId).filter(Objects::nonNull).distinct()
                .forEach(id -> projetRepository.findById(id).ifPresent(p -> projets.put(p.getId(), p)));
        projetsAnimes(auteurId).forEach(p -> projets.putIfAbsent(p.getId(), p));
        return new ArrayList<>(projets.values());
    }

    /** Techniciens des périmètres des projets dont l'utilisateur est le coordonnateur. */
    private Set<UUID> techniciensDesProjets(UUID coordonnateurId) {
        return projetRepository.findByChefProjetId(coordonnateurId).stream()
                .flatMap(p -> perimetreRepository.findByProjetIdAndIsDeletedFalseOrderByNomAsc(p.getId()).stream())
                .map(Perimetre::getTechnicienId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private SuiviMensuel suiviExistant(UUID id) {
        return suiviRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));
    }

    private void enregistrerFichier(SuiviMensuel suivi, MultipartFile fichier, String extension) {
        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Lecture du fichier impossible");
        }
        String chemin = "suivis/" + suivi.getId() + "/" + UUID.randomUUID() + "." + extension;
        stockage.deposer(chemin, contenu, TYPES_WORD.get(extension));
        suivi.setFichierNom(nomPropre(fichier.getOriginalFilename()));
        suivi.setFichierChemin(chemin);
        suivi.setFichierTaille(fichier.getSize());
    }

    private static String verifierWord(MultipartFile fichier) {
        if (fichier.getSize() > TAILLE_FICHIER_MAX) {
            throw new BusinessException("Fichier trop volumineux (10 Mo maximum)");
        }
        String extension = extension(fichier.getOriginalFilename());
        if (!TYPES_WORD.containsKey(extension)) {
            throw new BusinessException("Le rapport doit être un fichier Word (.doc ou .docx)");
        }
        return extension;
    }

    private static String extension(String nom) {
        if (nom == null) {
            return "";
        }
        int point = nom.lastIndexOf('.');
        return point < 0 ? "" : nom.substring(point + 1).toLowerCase(Locale.ROOT);
    }

    private static String nomPropre(String nom) {
        String n = nom == null ? "rapport" : nom.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        if (n.isEmpty()) {
            n = "rapport";
        }
        return n.length() > 255 ? n.substring(n.length() - 255) : n;
    }

    /**
     * Équipe encadrée : techniciens des périmètres supervisés et animateurs des projets coordonnés,
     * avec l'avancement de leurs activités.
     */
    @Transactional(readOnly = true)
    public List<MembreEquipe> equipe(User user) {
        Map<UUID, List<Perimetre>> parTechnicien = techniciensEncadres(user.getId());
        Map<UUID, List<Projet>> parAnimateur = animateursEncadres(user.getId());
        if (parTechnicien.isEmpty() && parAnimateur.isEmpty()) {
            return List.of();
        }
        Set<UUID> membres = new java.util.HashSet<>(parTechnicien.keySet());
        membres.addAll(parAnimateur.keySet());
        Map<UUID, List<SuiviMensuel>> suivis = suiviRepository
                .findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(membres).stream()
                .collect(Collectors.groupingBy(SuiviMensuel::getTechnicienId));
        String moisCourant = YearMonth.now().toString();
        LocalDate jour = LocalDate.now();
        List<MembreEquipe> equipe = new ArrayList<>();
        Map<UUID, List<Activite>> activitesParMembre = new LinkedHashMap<>();
        Map<UUID, List<String>> lieuxParMembre = new LinkedHashMap<>();
        parTechnicien.forEach((technicienId, perimetres) -> {
            activitesParMembre.put(technicienId, activiteRepository
                    .findByPerimetreIdInAndIsDeletedFalse(perimetres.stream().map(Perimetre::getId).toList()));
            lieuxParMembre.put(technicienId, perimetres.stream().map(Perimetre::getNom).toList());
        });
        parAnimateur.forEach((animateurId, projets) -> {
            Set<UUID> ids = projets.stream().map(Projet::getId).collect(Collectors.toSet());
            List<Activite> siennes = activiteRepository.findByResponsableIdAndIsDeletedFalse(animateurId).stream()
                    .filter(a -> ids.contains(a.getProjetId())).toList();
            activitesParMembre.merge(animateurId, siennes, (x, y) -> {
                List<Activite> tout = new ArrayList<>(x);
                y.stream().filter(a -> !tout.contains(a)).forEach(tout::add);
                return tout;
            });
            lieuxParMembre.merge(animateurId, projets.stream().map(Projet::getCode).toList(), (x, y) -> {
                List<String> tout = new ArrayList<>(x);
                tout.addAll(y);
                return tout;
            });
        });
        activitesParMembre.forEach((technicienId, toutes) -> {
            List<String> perimetres = lieuxParMembre.getOrDefault(technicienId, List.of());
            List<Activite> activites = toutes.stream()
                    .filter(a -> a.getStatut() != StatutActivite.ANNULEE)
                    .toList();
            List<Activite> formations = activites.stream().filter(a -> a.getType() == TypeActivite.FORMATION).toList();
            int personnes = activites.stream()
                    .mapToInt(a -> a.getParticipants() == null ? 0 : a.getParticipants()).sum();
            List<SuiviMensuel> siens = suivis.getOrDefault(technicienId, List.of());
            SuiviMensuel planning = siens.stream()
                    .filter(s -> s.getType() == TypeSuivi.PLANNING && moisCourant.equals(s.getMois()))
                    .findFirst().orElse(null);
            SuiviMensuel rapport = siens.stream().filter(s -> s.getType() == TypeSuivi.RAPPORT)
                    .findFirst().orElse(null);
            equipe.add(new MembreEquipe(technicienId, nomDe(technicienId),
                    perimetres,
                    activites.size(),
                    (int) activites.stream().filter(a -> a.getStatut() == StatutActivite.REALISEE).count(),
                    (int) activites.stream().filter(a -> enRetard(a, jour)).count(),
                    (int) formations.stream().filter(a -> a.getStatut() == StatutActivite.REALISEE).count(),
                    personnes,
                    planning == null ? null : planning.getStatut(),
                    rapport == null ? null : rapport.getStatut(),
                    rapport == null ? null : rapport.getMois()));
        });
        equipe.sort(Comparator.comparing(m -> m.nom() == null ? "" : m.nom()));
        return equipe;
    }

    private List<Projet> projetsAnimes(UUID animateurId) {
        return animateurRepository.findByAnimateurIdAndIsDeletedFalse(animateurId).stream()
                .map(a -> projetRepository.findById(a.getProjetId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    /** Animateurs des projets dont l'utilisateur est le coordonnateur. */
    private Map<UUID, List<Projet>> animateursEncadres(UUID coordonnateurId) {
        Map<UUID, List<Projet>> parAnimateur = new LinkedHashMap<>();
        for (Projet projet : projetRepository.findByChefProjetId(coordonnateurId)) {
            for (AnimateurProjet a : animateurRepository.findByProjetIdAndIsDeletedFalse(projet.getId())) {
                parAnimateur.computeIfAbsent(a.getAnimateurId(), k -> new ArrayList<>()).add(projet);
            }
        }
        return parAnimateur;
    }

    private Map<UUID, List<Perimetre>> techniciensEncadres(UUID superviseurId) {
        Map<UUID, List<Perimetre>> parTechnicien = new LinkedHashMap<>();
        perimetreRepository.findBySuperviseurIdAndIsDeletedFalseOrderByNomAsc(superviseurId).stream()
                .filter(p -> p.getTechnicienId() != null)
                .forEach(p -> parTechnicien.computeIfAbsent(p.getTechnicienId(), k -> new ArrayList<>()).add(p));
        return parTechnicien;
    }

    private static boolean enRetard(Activite a, LocalDate jour) {
        if (a.getStatut() == StatutActivite.REALISEE || a.getStatut() == StatutActivite.ANNULEE) {
            return false;
        }
        return a.getStatut() == StatutActivite.EN_RETARD
                || (a.getDatePrevuFin() != null && a.getDatePrevuFin().isBefore(jour));
    }

    /** Activités prévues dans le mois sur les périmètres du technicien ou dont il est responsable. */
    private List<ActiviteDuMois> activitesDuMois(SuiviMensuel s) {
        YearMonth mois = YearMonth.parse(s.getMois());
        Map<UUID, String> perimetres = new LinkedHashMap<>();
        perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(s.getTechnicienId())
                .forEach(p -> perimetres.put(p.getId(), p.getNom()));
        Map<UUID, Activite> activites = new LinkedHashMap<>();
        activiteRepository.findByResponsableIdAndIsDeletedFalse(s.getTechnicienId())
                .forEach(a -> activites.put(a.getId(), a));
        if (!perimetres.isEmpty()) {
            activiteRepository.findByPerimetreIdInAndIsDeletedFalse(perimetres.keySet())
                    .forEach(a -> activites.putIfAbsent(a.getId(), a));
        }
        return activites.values().stream()
                .filter(a -> a.getDatePrevuDebut() != null && YearMonth.from(a.getDatePrevuDebut()).equals(mois))
                .sorted(Comparator.comparing(Activite::getDatePrevuDebut))
                .map(a -> new ActiviteDuMois(a.getId(), a.getTitre(), a.getDatePrevuDebut(),
                        a.getPerimetreId() == null ? null : perimetres.get(a.getPerimetreId()), a.getStatut()))
                .toList();
    }

    private String nomDe(UUID userId) {
        return userId == null ? null : userRepository.findById(userId)
                .map(u -> (Objects.toString(u.getPrenom(), "") + " " + Objects.toString(u.getNom(), "")).trim())
                .orElse(null);
    }

    private SuiviResponse toResponse(SuiviMensuel s, User user) {
        return new SuiviResponse(s.getId(), s.getType(), s.getMois(), s.getTechnicienId(), nomDe(s.getTechnicienId()),
                s.getContenu(), s.getStatut(), s.getCommentaire(), nomDe(s.getDecidePar()), s.getDateDecision(),
                s.getCreatedAt(), s.getType() == TypeSuivi.PLANNING ? activitesDuMois(s) : List.of(),
                nomDe(s.getVisePar()), s.getDateVisa(), s.getContenuInitial(),
                s.getProjetId(),
                s.getProjetId() == null ? null
                        : projetRepository.findById(s.getProjetId()).map(Projet::getCode).orElse(null),
                s.getFichierNom(), s.getFichierTaille(), s.getFichierInitialNom(),
                doitDecider(user, s));
    }
}
