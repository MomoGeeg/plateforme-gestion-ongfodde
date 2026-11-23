package ong.fodde.modules.projets.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.PerimetreDtos.PerimetreRequest;
import ong.fodde.modules.projets.dto.PerimetreDtos.PerimetreResponse;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Périmètres d'un projet. Le chef de projet (coordonnateur), le RP ou l'admin les créent et y
 * affectent un technicien terrain et son superviseur. Un périmètre n'appartient qu'à un projet.
 */
@Service
@Transactional
public class PerimetreService {

    static final String ROLE_TECHNICIEN = "ROLE_TECHNICIEN_TERRAIN";
    static final String ROLE_SUPERVISEUR = "ROLE_SUPERVISEUR";

    public record Intervenant(UUID id, String nom) {
    }

    public record Intervenants(List<Intervenant> techniciens, List<Intervenant> superviseurs) {
    }

    private final PerimetreRepository perimetreRepository;
    private final ProjetRepository projetRepository;
    private final ActiviteRepository activiteRepository;
    private final UserRepository userRepository;
    private final DroitsProjet droits;

    public PerimetreService(PerimetreRepository perimetreRepository, ProjetRepository projetRepository,
                            ActiviteRepository activiteRepository, UserRepository userRepository,
                            DroitsProjet droits) {
        this.perimetreRepository = perimetreRepository;
        this.projetRepository = projetRepository;
        this.activiteRepository = activiteRepository;
        this.userRepository = userRepository;
        this.droits = droits;
    }

    public PerimetreResponse creer(PerimetreRequest request, User user) {
        Projet projet = projet(request.projetId());
        verifierGestion(projet, user);
        Perimetre perimetre = new Perimetre();
        perimetre.setProjetId(projet.getId());
        remplir(perimetre, request);
        return toResponse(perimetreRepository.save(perimetre), projet);
    }

    public PerimetreResponse modifier(UUID id, PerimetreRequest request, User user) {
        Perimetre perimetre = perimetre(id);
        Projet projet = projet(perimetre.getProjetId());
        verifierGestion(projet, user);
        if (!perimetre.getProjetId().equals(request.projetId())) {
            throw new BusinessException("Un périmètre ne change pas de projet");
        }
        remplir(perimetre, request);
        return toResponse(perimetreRepository.save(perimetre), projet);
    }

    /** Suppression seulement si aucune activité n'y est rattachée. */
    public void supprimer(UUID id, User user) {
        Perimetre perimetre = perimetre(id);
        verifierGestion(projet(perimetre.getProjetId()), user);
        if (activiteRepository.countByPerimetreIdAndIsDeletedFalse(id) > 0) {
            throw new BusinessException("Ce périmètre a des activités : retirez-les d'abord");
        }
        perimetre.setIsDeleted(true);
        perimetreRepository.save(perimetre);
    }

    @Transactional(readOnly = true)
    public List<PerimetreResponse> listerParProjet(UUID projetId) {
        Projet projet = projet(projetId);
        return perimetreRepository.findByProjetIdAndIsDeletedFalseOrderByNomAsc(projetId).stream()
                .map(p -> toResponse(p, projet))
                .toList();
    }

    /** Périmètres suivis par l'utilisateur, comme technicien ou comme superviseur. */
    @Transactional(readOnly = true)
    public List<PerimetreResponse> mesPerimetres(User user) {
        Map<UUID, Perimetre> liste = new LinkedHashMap<>();
        perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(user.getId())
                .forEach(p -> liste.put(p.getId(), p));
        perimetreRepository.findBySuperviseurIdAndIsDeletedFalseOrderByNomAsc(user.getId())
                .forEach(p -> liste.putIfAbsent(p.getId(), p));
        return liste.values().stream()
                .map(p -> toResponse(p, projetRepository.findById(p.getProjetId()).orElse(null)))
                .toList();
    }

    /** Comptes actifs pouvant être affectés : techniciens terrain et superviseurs. */
    @Transactional(readOnly = true)
    public Intervenants intervenants() {
        return new Intervenants(comptes(ROLE_TECHNICIEN), comptes(ROLE_SUPERVISEUR));
    }

    private List<Intervenant> comptes(String role) {
        return userRepository.findDistinctByRoles_NomOrderByNomAscPrenomAsc(role).stream()
                .filter(u -> u.getStatut() == StatutUser.ACTIF)
                .map(u -> new Intervenant(u.getId(), nom(u)))
                .toList();
    }

    private void verifierGestion(Projet projet, User user) {
        if (!droits.estGestionnaire(projet, user)) {
            throw new AccessDeniedException("Seuls le chef de projet et le RP gèrent les périmètres");
        }
        if (projet.getStatut() == StatutProjet.CLOTURE) {
            throw new BusinessException("Le projet est clôturé : ses périmètres ne se modifient plus");
        }
    }

    private void remplir(Perimetre p, PerimetreRequest r) {
        if (r.superficieHa() != null && r.superficieHa().signum() < 0) {
            throw new BusinessException("La superficie ne peut pas être négative");
        }
        p.setNom(r.nom().trim());
        p.setVillage(r.village().trim());
        p.setCommune(vide(r.commune()));
        p.setDepartement(vide(r.departement()));
        p.setSuperficieHa(r.superficieHa());
        p.setTechnicienId(verifierRole(r.technicienId(), ROLE_TECHNICIEN, "Le technicien"));
        p.setSuperviseurId(verifierRole(r.superviseurId(), ROLE_SUPERVISEUR, "Le superviseur"));
        if ((r.latitude() == null) != (r.longitude() == null)) {
            throw new BusinessException("Indiquez la latitude et la longitude, ou aucune des deux");
        }
        if (r.latitude() != null && (r.latitude().abs().compareTo(java.math.BigDecimal.valueOf(90)) > 0
                || r.longitude().abs().compareTo(java.math.BigDecimal.valueOf(180)) > 0)) {
            throw new BusinessException("Coordonnées invalides : latitude entre -90 et 90, longitude entre -180 et 180");
        }
        p.setLatitude(r.latitude() == null ? null : r.latitude().setScale(6, java.math.RoundingMode.HALF_UP));
        p.setLongitude(r.longitude() == null ? null : r.longitude().setScale(6, java.math.RoundingMode.HALF_UP));
        String cultures = r.cultures() == null ? "" : r.cultures().stream()
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().replace(",", " "))
                .distinct()
                .collect(java.util.stream.Collectors.joining(", "));
        if (cultures.length() > 500) {
            throw new BusinessException("Liste de cultures trop longue");
        }
        p.setCultures(cultures.isEmpty() ? null : cultures);
    }

    /** Tous les périmètres, pour la carte du RP et du SE (et de l'administrateur). */
    @Transactional(readOnly = true)
    public List<PerimetreResponse> carte(User user) {
        if (!droits.aUnRole(user, "ROLE_RESPONSABLE_PROGRAMMES", "ROLE_SE", "ROLE_ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La carte des périmètres est réservée au RP et au SE");
        }
        Map<UUID, Projet> projets = new java.util.HashMap<>();
        return perimetreRepository.findByIsDeletedFalseOrderByNomAsc().stream()
                .map(p -> toResponse(p, projets.computeIfAbsent(p.getProjetId(),
                        id -> projetRepository.findById(id).orElse(null))))
                .toList();
    }

    private UUID verifierRole(UUID userId, String role, String qui) {
        if (userId == null) {
            return null;
        }
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(qui + " est introuvable"));
        if (!droits.aUnRole(u, role)) {
            throw new BusinessException(qui + " choisi n'a pas ce rôle sur la plateforme");
        }
        return userId;
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nom(User u) {
        return (u.getPrenom() + " " + u.getNom()).trim();
    }

    private String nomDe(UUID userId) {
        return userId == null ? null : userRepository.findById(userId).map(PerimetreService::nom).orElse(null);
    }

    /** Périmètre existant (non supprimé). */
    public Perimetre perimetre(UUID id) {
        return perimetreRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Périmètre introuvable : " + id));
    }

    private Projet projet(UUID id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + id));
    }

    private PerimetreResponse toResponse(Perimetre p, Projet projet) {
        return new PerimetreResponse(p.getId(), p.getProjetId(),
                projet == null ? null : projet.getCode(), projet == null ? null : projet.getTitre(),
                p.getNom(), p.getVillage(), p.getCommune(), p.getDepartement(), p.getSuperficieHa(),
                p.getTechnicienId(), nomDe(p.getTechnicienId()),
                p.getSuperviseurId(), nomDe(p.getSuperviseurId()),
                p.getId() == null ? 0 : activiteRepository.countByPerimetreIdAndIsDeletedFalse(p.getId()),
                p.getLatitude(), p.getLongitude(),
                p.getCultures() == null ? List.of()
                        : java.util.Arrays.stream(p.getCultures().split(",")).map(String::trim)
                                .filter(c -> !c.isEmpty()).toList());
    }
}
