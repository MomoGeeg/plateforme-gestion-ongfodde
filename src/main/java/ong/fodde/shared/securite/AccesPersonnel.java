package ong.fodde.shared.securite;

import java.util.Arrays;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.rh.repository.EmployeRepository;

/**
 * Contrôle d'accès aux données personnelles (congés, contrat, évaluations,
 * avances, rapports…) : la personne concernée y a accès, ainsi que les rôles
 * de gestion indiqués. Tout autre utilisateur reçoit un 403.
 */
@Component
public class AccesPersonnel {

    private final EmployeRepository employeRepository;

    public AccesPersonnel(EmployeRepository employeRepository) {
        this.employeRepository = employeRepository;
    }

    /** Vrai si l'utilisateur possède au moins un des rôles. */
    public boolean aUnRole(User user, String... roles) {
        return user.getRoles().stream().anyMatch(r -> Arrays.asList(roles).contains(r.getNom()));
    }

    /** L'utilisateur doit être la personne visée (identifiant de compte) ou avoir un rôle de gestion. */
    public void verifierUtilisateur(UUID userIdVise, User user, String... rolesGestion) {
        if (user.getId().equals(userIdVise) || aUnRole(user, rolesGestion)) {
            return;
        }
        throw new AccessDeniedException("Ces données concernent un autre utilisateur");
    }

    /** L'utilisateur doit être l'employé visé (fiche liée à son compte) ou avoir un rôle de gestion. */
    public void verifierEmploye(UUID employeId, User user, String... rolesGestion) {
        if (aUnRole(user, rolesGestion)) {
            return;
        }
        boolean estLuiMeme = employeRepository.findByUserId(user.getId())
                .map(e -> e.getId().equals(employeId))
                .orElse(false);
        if (!estLuiMeme) {
            throw new AccessDeniedException("Ces données concernent un autre employé");
        }
    }
}
