package ong.fodde.modules.se.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.se.dto.IndicateurRequest;
import ong.fodde.modules.se.dto.IndicateurResponse;
import ong.fodde.modules.se.entity.Indicateur;
import ong.fodde.modules.se.repository.IndicateurRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class IndicateurService {

    /** Longueur maximale de la colonne indicateurs.code. */
    private static final int LONGUEUR_CODE = 30;

    private final IndicateurRepository indicateurRepository;
    private final ProjetRepository projetRepository;

    public IndicateurService(IndicateurRepository indicateurRepository, ProjetRepository projetRepository) {
        this.indicateurRepository = indicateurRepository;
        this.projetRepository = projetRepository;
    }

    public IndicateurResponse creerIndicateur(IndicateurRequest request) {
        String code = request.code() == null || request.code().isBlank()
            ? genererCode(request.projetId())
            : request.code().trim();
        if (indicateurRepository.existsByCode(code)) {
            throw new BusinessException("Un indicateur avec le code '" + code + "' existe déjà");
        }

        Indicateur indicateur = new Indicateur();
        indicateur.setProjetId(request.projetId());
        indicateur.setCode(code);
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

    /**
     * Code suivant pour un projet : « {code projet}-IND-{numéro} », ex. PROJ-2025-001-IND-03.
     * Le numéro suit le nombre d'indicateurs du projet ; on avance tant qu'il est déjà pris.
     */
    private String genererCode(UUID projetId) {
        Projet projet = projetRepository.findById(projetId)
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + projetId));
        String suffixe = "-IND-";
        String prefixe = projet.getCode();
        int place = LONGUEUR_CODE - suffixe.length() - 2;
        if (prefixe.length() > place) {
            prefixe = prefixe.substring(0, place);
        }
        int numero = indicateurRepository.findByProjetId(projetId).size() + 1;
        String code;
        do {
            code = prefixe + suffixe + String.format("%02d", numero++);
        } while (indicateurRepository.existsByCode(code));
        return code;
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
