package ong.fodde.modules.projets.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Qui peut monter un projet : le RP, le chef de projet désigné ou l'admin, tant qu'il est en brouillon. */
@Component
public class DroitsProjet {

    private final AccesPersonnel accesPersonnel;

    public DroitsProjet(AccesPersonnel accesPersonnel) {
        this.accesPersonnel = accesPersonnel;
    }

    public boolean estGestionnaire(Projet p, User u) {
        return accesPersonnel.aUnRole(u, "ROLE_RESPONSABLE_PROGRAMMES", "ROLE_ADMIN")
                || u.getId().equals(p.getChefProjetId());
    }

    public boolean peutMonter(Projet p, User u) {
        return p.getStatut() == StatutProjet.BROUILLON && estGestionnaire(p, u);
    }

    public void verifierMontage(Projet p, User u) {
        if (!estGestionnaire(p, u)) {
            throw new AccessDeniedException("Seuls le RP et le chef de projet montent ce projet");
        }
        if (p.getStatut() != StatutProjet.BROUILLON) {
            throw new BusinessException(
                    "Le projet n'est modifiable qu'en brouillon : demandez au RAF ou au SE de le renvoyer");
        }
    }

    public boolean aUnRole(User u, String... roles) {
        return accesPersonnel.aUnRole(u, roles);
    }
}
