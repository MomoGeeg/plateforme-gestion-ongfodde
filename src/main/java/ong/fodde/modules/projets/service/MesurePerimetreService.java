package ong.fodde.modules.projets.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.MesureDtos.IndicateurPerimetre;
import ong.fodde.modules.projets.dto.MesureDtos.IndicateursDuPerimetre;
import ong.fodde.modules.projets.dto.MesureDtos.MesureRequest;
import ong.fodde.modules.projets.dto.MesureDtos.MesureResponse;
import ong.fodde.modules.projets.entity.MesurePerimetre;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.MesurePerimetreRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.se.entity.Indicateur;
import ong.fodde.modules.se.repository.IndicateurRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Indicateurs suivis par périmètre : les indicateurs du projet, relevés sur chaque périmètre.
 * Saisie par le technicien ou le superviseur du périmètre, le chef de projet, le RP, le
 * responsable suivi-évaluation ou l'admin. La valeur atteinte du projet n'est pas modifiée :
 * le suivi-évaluation la consolide lui-même.
 */
@Service
@Transactional
public class MesurePerimetreService {

    private final MesurePerimetreRepository mesureRepository;
    private final PerimetreRepository perimetreRepository;
    private final ProjetRepository projetRepository;
    private final IndicateurRepository indicateurRepository;
    private final UserRepository userRepository;
    private final DroitsProjet droits;

    public MesurePerimetreService(MesurePerimetreRepository mesureRepository, PerimetreRepository perimetreRepository,
                                  ProjetRepository projetRepository, IndicateurRepository indicateurRepository,
                                  UserRepository userRepository, DroitsProjet droits) {
        this.mesureRepository = mesureRepository;
        this.perimetreRepository = perimetreRepository;
        this.projetRepository = projetRepository;
        this.indicateurRepository = indicateurRepository;
        this.userRepository = userRepository;
        this.droits = droits;
    }

    public MesureResponse enregistrer(UUID perimetreId, MesureRequest request, User user) {
        Perimetre perimetre = perimetre(perimetreId);
        Projet projet = projet(perimetre.getProjetId());
        if (!peutSaisir(perimetre, projet, user)) {
            throw new AccessDeniedException("Seuls le technicien, le superviseur, le chef de projet, le RP et le suivi-évaluation saisissent les indicateurs de ce périmètre");
        }
        if (projet.getStatut() == StatutProjet.CLOTURE) {
            throw new BusinessException("Le projet est clôturé : ses indicateurs ne se saisissent plus");
        }
        Indicateur indicateur = indicateurRepository.findById(request.indicateurId())
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur introuvable : " + request.indicateurId()));
        if (!indicateur.getProjetId().equals(projet.getId())) {
            throw new BusinessException("Cet indicateur n'appartient pas au projet du périmètre");
        }
        if (request.dateMesure().isAfter(LocalDate.now())) {
            throw new BusinessException("La date de la mesure ne peut pas être dans le futur");
        }
        if (request.valeur().signum() < 0) {
            throw new BusinessException("La valeur ne peut pas être négative");
        }
        MesurePerimetre m = new MesurePerimetre();
        m.setIndicateurId(indicateur.getId());
        m.setPerimetreId(perimetre.getId());
        m.setDateMesure(request.dateMesure());
        m.setValeur(request.valeur());
        m.setCommentaire(request.commentaire() == null || request.commentaire().isBlank() ? null : request.commentaire().trim());
        m.setSaisiPar(user.getId());
        return toResponse(mesureRepository.save(m));
    }

    /** Historique des mesures d'un périmètre, de la plus récente à la plus ancienne. */
    @Transactional(readOnly = true)
    public List<MesureResponse> mesures(UUID perimetreId) {
        perimetre(perimetreId);
        return mesureRepository.findByPerimetreIdAndIsDeletedFalseOrderByDateMesureDescCreatedAtDesc(perimetreId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public IndicateursDuPerimetre indicateurs(UUID perimetreId, User user) {
        Perimetre perimetre = perimetre(perimetreId);
        return resume(perimetre, projet(perimetre.getProjetId()), user);
    }

    /** Indicateurs de tous les périmètres suivis par l'utilisateur (technicien ou superviseur). */
    @Transactional(readOnly = true)
    public List<IndicateursDuPerimetre> mesIndicateurs(User user) {
        Map<UUID, Perimetre> liste = new LinkedHashMap<>();
        perimetreRepository.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(user.getId())
                .forEach(p -> liste.put(p.getId(), p));
        perimetreRepository.findBySuperviseurIdAndIsDeletedFalseOrderByNomAsc(user.getId())
                .forEach(p -> liste.putIfAbsent(p.getId(), p));
        return liste.values().stream()
                .map(p -> resume(p, projet(p.getProjetId()), user))
                .toList();
    }

    private IndicateursDuPerimetre resume(Perimetre perimetre, Projet projet, User user) {
        List<MesurePerimetre> mesures = mesureRepository
                .findByPerimetreIdAndIsDeletedFalseOrderByDateMesureDescCreatedAtDesc(perimetre.getId());
        List<IndicateurPerimetre> indicateurs = indicateurRepository.findByProjetId(projet.getId()).stream()
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .sorted(Comparator.comparing(Indicateur::getCode, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(i -> {
                    List<MesurePerimetre> siennes = mesures.stream()
                            .filter(m -> m.getIndicateurId().equals(i.getId())).toList();
                    MesurePerimetre derniere = siennes.isEmpty() ? null : siennes.get(0);
                    return new IndicateurPerimetre(i.getId(), i.getCode(), i.getLibelle(), i.getUnite(),
                            i.getValeurCible(),
                            derniere == null ? null : derniere.getValeur(),
                            derniere == null ? null : derniere.getDateMesure(),
                            siennes.size());
                })
                .toList();
        return new IndicateursDuPerimetre(perimetre.getId(), perimetre.getNom(), perimetre.getVillage(),
                peutSaisir(perimetre, projet, user), indicateurs);
    }

    boolean peutSaisir(Perimetre perimetre, Projet projet, User user) {
        return user.getId().equals(perimetre.getTechnicienId())
                || user.getId().equals(perimetre.getSuperviseurId())
                || droits.estGestionnaire(projet, user)
                || droits.aUnRole(user, "ROLE_RESPONSABLE_SE");
    }

    private Perimetre perimetre(UUID id) {
        return perimetreRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Périmètre introuvable : " + id));
    }

    private Projet projet(UUID id) {
        return projetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + id));
    }

    private MesureResponse toResponse(MesurePerimetre m) {
        String nom = m.getSaisiPar() == null ? null : userRepository.findById(m.getSaisiPar())
                .map(u -> (u.getPrenom() + " " + u.getNom()).trim()).orElse(null);
        return new MesureResponse(m.getId(), m.getIndicateurId(), m.getPerimetreId(), m.getDateMesure(),
                m.getValeur(), m.getCommentaire(), nom);
    }
}
