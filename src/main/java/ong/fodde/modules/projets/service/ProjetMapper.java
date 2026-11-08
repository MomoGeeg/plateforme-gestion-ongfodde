package ong.fodde.modules.projets.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.ProjetResponse;
import ong.fodde.modules.projets.entity.Projet;

/** Conversion d'un projet en réponse (noms du responsable et du chef de projet). */
@Component
public class ProjetMapper {

    private final UserRepository userRepository;

    public ProjetMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String nom(UUID userId) {
        return userId == null ? null
                : userRepository.findById(userId).map(u -> u.getPrenom() + " " + u.getNom()).orElse(null);
    }

    public ProjetResponse toResponse(Projet p) {
        return new ProjetResponse(
            p.getId(),
            p.getCode(),
            p.getTitre(),
            p.getDescription(),
            p.getObjectifGeneral(),
            p.getZoneGeographique(),
            p.getDateDebut(),
            p.getDateFin(),
            p.getBudgetTotal(),
            p.getDevise(),
            p.getStatut(),
            p.getResponsableId(),
            p.getCreatedAt(),
            nom(p.getResponsableId()),
            p.getChefProjetId(),
            nom(p.getChefProjetId()),
            p.getMotifRenvoi()
        );
    }
}
