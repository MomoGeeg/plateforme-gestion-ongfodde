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
    private final ong.fodde.modules.financier.repository.DepenseRepository depenseRepository;

    public ProjetMapper(UserRepository userRepository,
                        ong.fodde.modules.financier.repository.DepenseRepository depenseRepository) {
        this.userRepository = userRepository;
        this.depenseRepository = depenseRepository;
    }

    public String nom(UUID userId) {
        return userId == null ? null
                : userRepository.findById(userId).map(u -> u.getPrenom() + " " + u.getNom()).orElse(null);
    }

    public ProjetResponse toResponse(Projet p) {
        java.math.BigDecimal depense = p.getId() == null ? null : depenseRepository.sumValideesByProjet(p.getId());
        if (depense == null) {
            depense = java.math.BigDecimal.ZERO;
        }
        java.math.BigDecimal budget = p.getBudgetTotal() != null ? p.getBudgetTotal() : java.math.BigDecimal.ZERO;
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
            p.getMotifRenvoi(),
            p.getBailleurs().isEmpty() ? null
                    : p.getBailleurs().stream().map(b -> b.getNom()).sorted().collect(java.util.stream.Collectors.joining(", ")),
            depense,
            budget.subtract(depense)
        );
    }
}
