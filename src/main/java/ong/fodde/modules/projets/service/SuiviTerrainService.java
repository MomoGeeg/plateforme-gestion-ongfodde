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
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.SuiviMensuelRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Suivi mensuel du terrain : le technicien remet son planning et son rapport du mois, le superviseur
 * de ses périmètres les valide ou les renvoie à revoir. Le superviseur voit aussi son équipe.
 */
@Service
@Transactional
public class SuiviTerrainService {

    private final SuiviMensuelRepository suiviRepository;
    private final PerimetreRepository perimetreRepository;
    private final ActiviteRepository activiteRepository;
    private final UserRepository userRepository;
    private final DroitsProjet droits;

    public SuiviTerrainService(SuiviMensuelRepository suiviRepository, PerimetreRepository perimetreRepository,
                               ActiviteRepository activiteRepository, UserRepository userRepository,
                               DroitsProjet droits) {
        this.suiviRepository = suiviRepository;
        this.perimetreRepository = perimetreRepository;
        this.activiteRepository = activiteRepository;
        this.userRepository = userRepository;
        this.droits = droits;
    }

    /**
     * Remise du planning ou du rapport d'un mois. Une nouvelle remise remplace la précédente tant
     * qu'elle n'est pas validée.
     */
    public SuiviResponse soumettre(SuiviRequest request, User user) {
        List<Perimetre> perimetres = perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(user.getId());
        if (perimetres.isEmpty()) {
            throw new BusinessException("Aucun périmètre ne vous est affecté");
        }
        if (perimetres.stream().allMatch(p -> p.getSuperviseurId() == null)) {
            throw new BusinessException("Aucun superviseur n'est affecté à vos périmètres");
        }
        YearMonth mois = YearMonth.parse(request.mois());
        if (request.type() == TypeSuivi.RAPPORT && mois.isAfter(YearMonth.now())) {
            throw new BusinessException("Le rapport d'un mois à venir ne peut pas encore être remis");
        }
        SuiviMensuel suivi = suiviRepository
                .findByTechnicienIdAndTypeAndMoisAndIsDeletedFalse(user.getId(), request.type(), request.mois())
                .orElseGet(SuiviMensuel::new);
        if (suivi.getStatut() == StatutSuivi.VALIDE && suivi.getId() != null) {
            throw new BusinessException("Ce document est déjà validé par le superviseur");
        }
        suivi.setType(request.type());
        suivi.setTechnicienId(user.getId());
        suivi.setMois(request.mois());
        suivi.setContenu(request.contenu().trim());
        suivi.setStatut(StatutSuivi.SOUMIS);
        suivi.setDecidePar(null);
        suivi.setDateDecision(null);
        return toResponse(suiviRepository.save(suivi));
    }

    /** Plannings et rapports remis par le technicien connecté. */
    @Transactional(readOnly = true)
    public List<SuiviResponse> mesSuivis(User user) {
        return suiviRepository.findByTechnicienIdAndIsDeletedFalseOrderByMoisDescTypeAsc(user.getId()).stream()
                .map(this::toResponse).toList();
    }

    /** Plannings et rapports des techniciens encadrés par le superviseur connecté. */
    @Transactional(readOnly = true)
    public List<SuiviResponse> suivisEquipe(User user) {
        Set<UUID> techniciens = techniciensEncadres(user.getId()).keySet();
        if (techniciens.isEmpty()) {
            return List.of();
        }
        return suiviRepository.findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(techniciens).stream()
                .map(this::toResponse).toList();
    }

    /** Validation par le superviseur ; un renvoi « à revoir » exige un commentaire. */
    public SuiviResponse decider(UUID id, DecisionRequest request, User user) {
        SuiviMensuel suivi = suiviRepository.findById(id)
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + id));
        if (!estSuperviseurDe(user, suivi.getTechnicienId())) {
            throw new AccessDeniedException("Seul le superviseur du technicien valide ses documents");
        }
        if (suivi.getStatut() != StatutSuivi.SOUMIS) {
            throw new BusinessException("Ce document a déjà été traité");
        }
        boolean valide = Boolean.TRUE.equals(request.valide());
        String commentaire = request.commentaire() == null || request.commentaire().isBlank()
                ? null : request.commentaire().trim();
        if (!valide && commentaire == null) {
            throw new BusinessException("Indiquez ce qui est à revoir");
        }
        suivi.setStatut(valide ? StatutSuivi.VALIDE : StatutSuivi.A_REVOIR);
        suivi.setCommentaire(commentaire);
        suivi.setDecidePar(user.getId());
        suivi.setDateDecision(LocalDateTime.now());
        return toResponse(suiviRepository.save(suivi));
    }

    /** Techniciens encadrés, avec l'avancement des activités des périmètres supervisés. */
    @Transactional(readOnly = true)
    public List<MembreEquipe> equipe(User user) {
        Map<UUID, List<Perimetre>> parTechnicien = techniciensEncadres(user.getId());
        if (parTechnicien.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<SuiviMensuel>> suivis = suiviRepository
                .findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(parTechnicien.keySet()).stream()
                .collect(Collectors.groupingBy(SuiviMensuel::getTechnicienId));
        String moisCourant = YearMonth.now().toString();
        LocalDate jour = LocalDate.now();
        List<MembreEquipe> equipe = new ArrayList<>();
        parTechnicien.forEach((technicienId, perimetres) -> {
            List<Activite> activites = activiteRepository
                    .findByPerimetreIdInAndIsDeletedFalse(perimetres.stream().map(Perimetre::getId).toList()).stream()
                    .filter(a -> a.getStatut() != StatutActivite.ANNULEE)
                    .toList();
            List<Activite> formations = activites.stream().filter(a -> a.getType() == TypeActivite.FORMATION).toList();
            List<SuiviMensuel> siens = suivis.getOrDefault(technicienId, List.of());
            SuiviMensuel planning = siens.stream()
                    .filter(s -> s.getType() == TypeSuivi.PLANNING && moisCourant.equals(s.getMois()))
                    .findFirst().orElse(null);
            SuiviMensuel rapport = siens.stream().filter(s -> s.getType() == TypeSuivi.RAPPORT)
                    .findFirst().orElse(null);
            equipe.add(new MembreEquipe(technicienId, nomDe(technicienId),
                    perimetres.stream().map(Perimetre::getNom).toList(),
                    activites.size(),
                    (int) activites.stream().filter(a -> a.getStatut() == StatutActivite.REALISEE).count(),
                    (int) activites.stream().filter(a -> enRetard(a, jour)).count(),
                    (int) formations.stream().filter(a -> a.getStatut() == StatutActivite.REALISEE).count(),
                    formations.stream().mapToInt(a -> a.getParticipants() == null ? 0 : a.getParticipants()).sum(),
                    planning == null ? null : planning.getStatut(),
                    rapport == null ? null : rapport.getStatut(),
                    rapport == null ? null : rapport.getMois()));
        });
        equipe.sort(Comparator.comparing(m -> m.nom() == null ? "" : m.nom()));
        return equipe;
    }

    /** Vrai si l'utilisateur supervise au moins un périmètre de ce technicien. */
    public boolean estSuperviseurDe(User user, UUID technicienId) {
        if (droits.aUnRole(user, "ROLE_ADMIN")) {
            return true;
        }
        return perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(technicienId).stream()
                .anyMatch(p -> user.getId().equals(p.getSuperviseurId()));
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

    private SuiviResponse toResponse(SuiviMensuel s) {
        return new SuiviResponse(s.getId(), s.getType(), s.getMois(), s.getTechnicienId(), nomDe(s.getTechnicienId()),
                s.getContenu(), s.getStatut(), s.getCommentaire(), nomDe(s.getDecidePar()), s.getDateDecision(),
                s.getCreatedAt(), s.getType() == TypeSuivi.PLANNING ? activitesDuMois(s) : List.of());
    }
}
