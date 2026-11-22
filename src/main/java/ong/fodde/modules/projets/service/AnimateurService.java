package ong.fodde.modules.projets.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.AnimateurDtos.AnimateurResponse;
import ong.fodde.modules.projets.dto.AnimateurDtos.ProjetAnimateur;
import ong.fodde.modules.projets.entity.AnimateurProjet;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.AnimateurProjetRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Animateurs rattachés aux projets. Le chef de projet (coordonnateur) ou le RP les affecte ; un
 * animateur peut intervenir sur plusieurs projets.
 */
@Service
@Transactional
public class AnimateurService {

    static final String ROLE_ANIMATEUR = "ROLE_ANIMATEUR";

    private final AnimateurProjetRepository animateurRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final DroitsProjet droits;

    public AnimateurService(AnimateurProjetRepository animateurRepository, ProjetRepository projetRepository,
                            UserRepository userRepository, DroitsProjet droits) {
        this.animateurRepository = animateurRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
        this.droits = droits;
    }

    @Transactional(readOnly = true)
    public List<AnimateurResponse> animateursDuProjet(UUID projetId) {
        projet(projetId);
        return animateurRepository.findByProjetIdAndIsDeletedFalse(projetId).stream()
                .map(a -> new AnimateurResponse(a.getAnimateurId(), nomDe(a.getAnimateurId())))
                .sorted(Comparator.comparing(a -> Objects.toString(a.nom(), "")))
                .toList();
    }

    public AnimateurResponse affecter(UUID projetId, UUID animateurId, User user) {
        Projet projet = projet(projetId);
        verifierGestion(projet, user);
        User animateur = userRepository.findById(animateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Animateur introuvable : " + animateurId));
        if (!droits.aUnRole(animateur, ROLE_ANIMATEUR)) {
            throw new BusinessException("Ce compte n'a pas le rôle animateur");
        }
        if (animateurRepository.existsByProjetIdAndAnimateurIdAndIsDeletedFalse(projetId, animateurId)) {
            throw new BusinessException("Cet animateur est déjà sur le projet");
        }
        AnimateurProjet a = new AnimateurProjet();
        a.setProjetId(projetId);
        a.setAnimateurId(animateurId);
        animateurRepository.save(a);
        return new AnimateurResponse(animateurId, nom(animateur));
    }

    /** Retrait du projet ; ses activités passées restent. */
    public void retirer(UUID projetId, UUID animateurId, User user) {
        verifierGestion(projet(projetId), user);
        AnimateurProjet a = animateurRepository.findByProjetIdAndAnimateurIdAndIsDeletedFalse(projetId, animateurId)
                .orElseThrow(() -> new ResourceNotFoundException("Cet animateur n'est pas sur le projet"));
        a.setIsDeleted(true);
        animateurRepository.save(a);
    }

    /** Comptes actifs ayant le rôle animateur. */
    @Transactional(readOnly = true)
    public List<AnimateurResponse> disponibles() {
        return userRepository.findDistinctByRoles_NomOrderByNomAscPrenomAsc(ROLE_ANIMATEUR).stream()
                .filter(u -> u.getStatut() == StatutUser.ACTIF)
                .map(u -> new AnimateurResponse(u.getId(), nom(u)))
                .toList();
    }

    /** Projets de l'animateur connecté. */
    @Transactional(readOnly = true)
    public List<ProjetAnimateur> mesProjets(User user) {
        return animateurRepository.findByAnimateurIdAndIsDeletedFalse(user.getId()).stream()
                .map(a -> projetRepository.findById(a.getProjetId()).orElse(null))
                .filter(Objects::nonNull)
                .map(p -> new ProjetAnimateur(p.getId(), p.getCode(), p.getTitre(), p.getStatut(),
                        nomDe(p.getChefProjetId())))
                .toList();
    }

    private void verifierGestion(Projet projet, User user) {
        if (!droits.estGestionnaire(projet, user)) {
            throw new AccessDeniedException("Seuls le chef de projet et le RP affectent les animateurs");
        }
        if (projet.getStatut() == StatutProjet.CLOTURE) {
            throw new BusinessException("Le projet est clôturé");
        }
    }

    private Projet projet(UUID id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + id));
    }

    private static String nom(User u) {
        return (Objects.toString(u.getPrenom(), "") + " " + Objects.toString(u.getNom(), "")).trim();
    }

    private String nomDe(UUID userId) {
        return userId == null ? null : userRepository.findById(userId).map(AnimateurService::nom).orElse(null);
    }
}
