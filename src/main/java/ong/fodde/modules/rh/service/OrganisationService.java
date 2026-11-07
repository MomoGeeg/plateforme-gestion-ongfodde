package ong.fodde.modules.rh.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.rh.dto.OrganisationDtos.NoeudOrganigramme;
import ong.fodde.modules.rh.dto.OrganisationDtos.PropositionRattachement;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.entity.Pole;
import ong.fodde.modules.rh.entity.StatutEmploye;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Organisation de l'ONG, d'après l'organigramme : pôle et supérieur direct (N+1) de chacun.
 *
 * <pre>
 * Secrétaire exécutif ── chargé de suivi-évaluation
 *   ├─ RAF : secrétariat, comptabilité, logistique, RH
 *   ├─ RP  : chargés de programme, techniciens génie civil et rural
 *   └─ RED/CAP/COM : éducation au développement, capitalisation, communication
 * </pre>
 */
@Service
@Transactional(readOnly = true)
public class OrganisationService {

    /** Pôle d'un rôle et rôle de son chef ; du plus haut au plus bas (le premier rôle trouvé l'emporte). */
    record Rattachement(Pole pole, String roleDuChef) {
    }

    static final Map<String, Rattachement> RATTACHEMENTS = new LinkedHashMap<>();

    static {
        RATTACHEMENTS.put("ROLE_SE", new Rattachement(Pole.DIRECTION, null));
        RATTACHEMENTS.put("ROLE_RAF", new Rattachement(Pole.RAF, "ROLE_SE"));
        RATTACHEMENTS.put("ROLE_RESPONSABLE_PROGRAMMES", new Rattachement(Pole.PROGRAMMES, "ROLE_SE"));
        RATTACHEMENTS.put("ROLE_RESPONSABLE_SE", new Rattachement(Pole.DIRECTION, "ROLE_SE"));
        RATTACHEMENTS.put("ROLE_COMPTABLE", new Rattachement(Pole.RAF, "ROLE_RAF"));
        RATTACHEMENTS.put("ROLE_RESPONSABLE_LOGISTIQUE", new Rattachement(Pole.RAF, "ROLE_RAF"));
        RATTACHEMENTS.put("ROLE_RH", new Rattachement(Pole.RAF, "ROLE_RAF"));
        RATTACHEMENTS.put("ROLE_CHARGE_PROGRAMME", new Rattachement(Pole.PROGRAMMES, "ROLE_RESPONSABLE_PROGRAMMES"));
        RATTACHEMENTS.put("ROLE_TECHNICIEN_TERRAIN", new Rattachement(Pole.PROGRAMMES, "ROLE_RESPONSABLE_PROGRAMMES"));
        // Le responsable RED/CAP/COM n'a pas de rôle dans l'application : les RH choisissent le chef.
        RATTACHEMENTS.put("ROLE_CHARGE_COMMUNICATION", new Rattachement(Pole.RED_CAP_COM, null));
    }

    private static final Map<String, String> NOMS_CHEFS = Map.of(
            "ROLE_SE", "Secrétaire exécutif",
            "ROLE_RAF", "RAF",
            "ROLE_RESPONSABLE_PROGRAMMES", "Responsable des programmes");

    private final EmployeRepository employeRepository;
    private final UserRepository userRepository;

    public OrganisationService(EmployeRepository employeRepository, UserRepository userRepository) {
        this.employeRepository = employeRepository;
        this.userRepository = userRepository;
    }

    /** Rattachement d'après les rôles : le plus haut rôle connu l'emporte. */
    static Optional<Rattachement> rattachementPour(Collection<String> roles) {
        return RATTACHEMENTS.entrySet().stream()
                .filter(e -> roles.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    /**
     * Pôle et supérieur proposés pour la personne qui a ce compte. Le supérieur est la fiche
     * active de la seule personne qui a le rôle du chef ; s'il y en a zéro ou plusieurs,
     * il reste à choisir.
     */
    public PropositionRattachement proposer(UUID userId) {
        User compte = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte utilisateur introuvable : " + userId));
        List<String> roles = compte.getRoles().stream().map(Role::getNom).toList();
        Optional<Rattachement> r = rattachementPour(roles);
        if (r.isEmpty()) {
            return new PropositionRattachement(null, null, null,
                    "Aucun rôle métier sur ce compte : choisissez le pôle et le supérieur.");
        }
        Pole pole = r.get().pole();
        String roleChef = r.get().roleDuChef();
        if (roleChef == null) {
            return new PropositionRattachement(pole, null, null, pole == Pole.DIRECTION
                    ? "Le Secrétaire exécutif n'a pas de supérieur dans l'application."
                    : "Choisissez le responsable du pôle comme supérieur.");
        }
        List<Employe> chefs = userRepository.findAll().stream()
                .filter(u -> u.getStatut() != StatutUser.INACTIF && !u.getId().equals(userId))
                .filter(u -> u.getRoles().stream().anyMatch(role -> roleChef.equals(role.getNom())))
                .map(u -> employeRepository.findByUserId(u.getId()))
                .flatMap(Optional::stream)
                .filter(e -> e.getStatut() != StatutEmploye.PARTI)
                .toList();
        String libelleChef = NOMS_CHEFS.getOrDefault(roleChef, roleChef);
        if (chefs.size() == 1) {
            Employe chef = chefs.get(0);
            return new PropositionRattachement(pole, chef.getId(), chef.getPrenom() + " " + chef.getNom(),
                    "Proposé d'après le rôle : " + libelleChef + ".");
        }
        return new PropositionRattachement(pole, null, null, chefs.isEmpty()
                ? "Aucune fiche pour le " + libelleChef + " : choisissez le supérieur."
                : chefs.size() + " personnes ont le rôle " + libelleChef + " : choisissez le supérieur.");
    }

    /** Employés présents (pas ceux qui ont quitté l'ONG), triés par pôle puis par nom : le frontend construit l'arbre. */
    public List<NoeudOrganigramme> organigramme() {
        return employeRepository.findAll().stream()
                .filter(e -> e.getStatut() != StatutEmploye.PARTI && !Boolean.TRUE.equals(e.getIsDeleted()))
                .sorted(Comparator.comparing((Employe e) -> e.getPole() == null ? 99 : e.getPole().ordinal())
                        .thenComparing(e -> e.getNom().toLowerCase())
                        .thenComparing(e -> e.getPrenom().toLowerCase()))
                .map(e -> new NoeudOrganigramme(e.getId(), e.getNom(), e.getPrenom(), e.getPoste(),
                        e.getPole(), e.getSuperieurId(), e.getUserId() != null))
                .toList();
    }
}
