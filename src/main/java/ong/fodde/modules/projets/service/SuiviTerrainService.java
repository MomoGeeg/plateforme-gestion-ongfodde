package ong.fodde.modules.projets.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.SuiviDtos.ActiviteDuMois;
import ong.fodde.modules.projets.dto.SuiviDtos.DecisionRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.MembreEquipe;
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
 * Suivi mensuel du terrain : le technicien (ou l'animateur) remet son planning et son rapport du
 * mois ; le superviseur de ses périmètres le vise (en corrigeant le texte au besoin) et le transmet
 * au coordonnateur du projet, qui le valide ou le renvoie à son auteur. Validé, le document est
 * visible du RAF, du RP, du SE et du suivi-évaluation.
 */
@Service
@Transactional
public class SuiviTerrainService {

    private final SuiviMensuelRepository suiviRepository;
    private final PerimetreRepository perimetreRepository;
    private final ActiviteRepository activiteRepository;
    private final UserRepository userRepository;
    private final AnimateurProjetRepository animateurRepository;
    private final ProjetRepository projetRepository;
    private final DroitsProjet droits;

    public SuiviTerrainService(SuiviMensuelRepository suiviRepository, PerimetreRepository perimetreRepository,
                               ActiviteRepository activiteRepository, UserRepository userRepository,
                               AnimateurProjetRepository animateurRepository, ProjetRepository projetRepository,
                               DroitsProjet droits) {
        this.suiviRepository = suiviRepository;
        this.perimetreRepository = perimetreRepository;
        this.activiteRepository = activiteRepository;
        this.userRepository = userRepository;
        this.animateurRepository = animateurRepository;
        this.projetRepository = projetRepository;
        this.droits = droits;
    }

    /**
     * Remise du planning ou du rapport d'un mois. Une nouvelle remise remplace la précédente tant
     * qu'elle n'est pas validée.
     */
    public SuiviResponse soumettre(SuiviRequest request, User user) {
        List<Perimetre> perimetres = perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(user.getId());
        List<Projet> projets = projetsAnimes(user.getId());
        if (perimetres.isEmpty() && projets.isEmpty()) {
            throw new BusinessException("Aucun périmètre ni projet ne vous est affecté");
        }
        if (perimetres.stream().allMatch(p -> p.getSuperviseurId() == null)
                && projets.stream().allMatch(p -> p.getChefProjetId() == null)) {
            throw new BusinessException("Personne n'est désigné pour valider vos documents");
        }
        YearMonth mois = YearMonth.parse(request.mois());
        if (request.type() == TypeSuivi.RAPPORT && mois.isAfter(YearMonth.now())) {
            throw new BusinessException("Le rapport d'un mois à venir ne peut pas encore être remis");
        }
        SuiviMensuel suivi = suiviRepository
                .findByTechnicienIdAndTypeAndMoisAndIsDeletedFalse(user.getId(), request.type(), request.mois())
                .orElseGet(SuiviMensuel::new);
        if (suivi.getId() != null && suivi.getStatut() == StatutSuivi.TRANSMIS) {
            throw new BusinessException("Ce document est déjà validé par le superviseur et transmis au coordonnateur");
        }
        if (suivi.getId() != null && suivi.getStatut() == StatutSuivi.VALIDE) {
            throw new BusinessException("Ce document est déjà validé");
        }
        suivi.setType(request.type());
        suivi.setTechnicienId(user.getId());
        suivi.setMois(request.mois());
        suivi.setContenu(request.contenu().trim());
        suivi.setContenuInitial(null);
        suivi.setStatut(StatutSuivi.SOUMIS);
        suivi.setCommentaire(null);
        suivi.setDecidePar(null);
        suivi.setDateDecision(null);
        suivi.setVisePar(null);
        suivi.setDateVisa(null);
        return toResponse(suiviRepository.save(suivi), user);
    }

    /** Plannings et rapports remis par le technicien connecté. */
    @Transactional(readOnly = true)
    public List<SuiviResponse> mesSuivis(User user) {
        return suiviRepository.findByTechnicienIdAndIsDeletedFalseOrderByMoisDescTypeAsc(user.getId()).stream()
                .map(s -> toResponse(s, user)).toList();
    }

    /**
     * Plannings et rapports suivis par l'utilisateur connecté : techniciens de ses périmètres
     * (superviseur), techniciens des périmètres et animateurs de ses projets (coordonnateur).
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
                .map(s -> toResponse(s, user)).toList();
    }

    /** Plannings et rapports validés par le coordonnateur : RAF, RP, SE et suivi-évaluation. */
    @Transactional(readOnly = true)
    public List<SuiviResponse> valides(User user) {
        return suiviRepository.findByStatutAndIsDeletedFalseOrderByDateDecisionDesc(StatutSuivi.VALIDE).stream()
                .map(s -> toResponse(s, user)).toList();
    }

    /**
     * Décision sur un document. Remis (SOUMIS) : le superviseur le vise, en corrigeant le texte au
     * besoin, et le transmet au coordonnateur du projet ; sans superviseur, le coordonnateur décide
     * directement. Transmis : le coordonnateur valide, et le document est diffusé. Un renvoi à
     * l'auteur exige un commentaire.
     */
    public SuiviResponse decider(UUID id, DecisionRequest request, User user) {
        SuiviMensuel suivi = suiviRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));
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
            Set<UUID> coordonnateurs = coordonnateursDe(suivi.getTechnicienId());
            if (!coordonnateurs.isEmpty() && !coordonnateurs.contains(user.getId())) {
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
     * des périmètres de l'auteur (à défaut, un coordonnateur de ses projets) ; un document transmis
     * attend un coordonnateur. L'auteur ne décide jamais de son propre document.
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
            return coordonnateursDe(s.getTechnicienId()).contains(user.getId());
        }
        Set<UUID> superviseurs = superviseursDe(s.getTechnicienId());
        return superviseurs.isEmpty()
                ? coordonnateursDe(s.getTechnicienId()).contains(user.getId())
                : superviseurs.contains(user.getId());
    }

    private Set<UUID> superviseursDe(UUID auteurId) {
        return perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(auteurId).stream()
                .map(Perimetre::getSuperviseurId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<UUID> coordonnateursDe(UUID auteurId) {
        return projetsDe(auteurId).stream()
                .map(Projet::getChefProjetId).filter(Objects::nonNull).collect(Collectors.toSet());
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
                projetsDe(s.getTechnicienId()).stream().map(Projet::getCode).filter(Objects::nonNull).toList(),
                doitDecider(user, s));
    }
}
