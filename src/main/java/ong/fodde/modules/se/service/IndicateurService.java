package ong.fodde.modules.se.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.se.dto.IndicateurRequest;
import ong.fodde.modules.se.dto.IndicateurResponse;
import ong.fodde.modules.se.entity.Indicateur;
import ong.fodde.modules.se.repository.IndicateurRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class IndicateurService {

    private final IndicateurRepository indicateurRepository;

    public IndicateurService(IndicateurRepository indicateurRepository) {
        this.indicateurRepository = indicateurRepository;
    }

    public IndicateurResponse creerIndicateur(IndicateurRequest request) {
        if (indicateurRepository.existsByCode(request.code())) {
            throw new BusinessException("Un indicateur avec le code '" + request.code() + "' existe déjà");
        }

        Indicateur indicateur = new Indicateur();
        indicateur.setProjetId(request.projetId());
        indicateur.setCode(request.code());
        indicateur.setLibelle(request.libelle());
        indicateur.setType(request.type());
        indicateur.setNiveau(request.niveau());
        indicateur.setUnite(request.unite());
        indicateur.setValeurBaseline(request.valeurBaseline());
        indicateur.setValeurCible(request.valeurCible());
        indicateur.setValeurAtteinte(BigDecimal.ZERO);
        indicateur.setFrequenceCollecte(request.frequenceCollecte());
        indicateur.setSourceVerification(request.sourceVerification());
        indicateur.setResponsableId(request.responsableId());

        Indicateur saved = indicateurRepository.save(indicateur);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IndicateurResponse> listerIndicateurs(UUID projetId) {
        return indicateurRepository.findByProjetId(projetId).stream()
            .map(this::toResponse)
            .toList();
    }

    public IndicateurResponse mettreAJourValeurAtteinte(UUID id, BigDecimal valeur) {
        Indicateur indicateur = indicateurRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Indicateur introuvable avec l'identifiant : " + id));

        indicateur.setValeurAtteinte(valeur);
        Indicateur saved = indicateurRepository.save(indicateur);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IndicateurResponse> listerTousIndicateurs() {
        return indicateurRepository.findAll().stream()
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getCode(), b.getCode()))
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public IndicateurResponse obtenirIndicateur(UUID id) {
        Indicateur indicateur = indicateurRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Indicateur introuvable avec l'identifiant : " + id));
        return toResponse(indicateur);
    }

    private IndicateurResponse toResponse(Indicateur ind) {
        Double tauxRealisation = 0.0;
        BigDecimal valeurAtteinte = ind.getValeurAtteinte() != null ? ind.getValeurAtteinte() : BigDecimal.ZERO;
        BigDecimal valeurCible = ind.getValeurCible();

        if (valeurCible != null && valeurCible.compareTo(BigDecimal.ZERO) > 0) {
            tauxRealisation = valeurAtteinte
                .multiply(BigDecimal.valueOf(100))
                .divide(valeurCible, 2, RoundingMode.HALF_UP)
                .doubleValue();
        }

        return new IndicateurResponse(
            ind.getId(),
            ind.getProjetId(),
            ind.getCode(),
            ind.getLibelle(),
            ind.getType(),
            ind.getNiveau(),
            ind.getUnite(),
            ind.getValeurBaseline(),
            valeurCible,
            valeurAtteinte,
            ind.getFrequenceCollecte(),
            ind.getSourceVerification(),
            ind.getResponsableId(),
            ind.getCreatedAt(),
            tauxRealisation
        );
    }
}
