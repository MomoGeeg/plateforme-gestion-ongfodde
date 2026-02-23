package ong.fodde.modules.se.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.se.dto.DonneeCollecteeRequest;
import ong.fodde.modules.se.dto.DonneeCollecteeResponse;
import ong.fodde.modules.se.dto.FormulaireRequest;
import ong.fodde.modules.se.dto.FormulaireResponse;
import ong.fodde.modules.se.entity.DonneeCollectee;
import ong.fodde.modules.se.entity.FormulaireCollecte;
import ong.fodde.modules.se.entity.StatutFormulaire;
import ong.fodde.modules.se.repository.ChampFormulaireRepository;
import ong.fodde.modules.se.repository.DonneeCollecteeRepository;
import ong.fodde.modules.se.repository.FormulaireCollecteRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class FormulaireService {

    private final FormulaireCollecteRepository formulaireRepository;
    private final ChampFormulaireRepository champRepository;
    private final DonneeCollecteeRepository donneeRepository;

    public FormulaireService(
            FormulaireCollecteRepository formulaireRepository,
            ChampFormulaireRepository champRepository,
            DonneeCollecteeRepository donneeRepository) {
        this.formulaireRepository = formulaireRepository;
        this.champRepository = champRepository;
        this.donneeRepository = donneeRepository;
    }

    public FormulaireResponse creerFormulaire(FormulaireRequest request) {
        FormulaireCollecte formulaire = new FormulaireCollecte();
        formulaire.setProjetId(request.projetId());
        formulaire.setTitre(request.titre());
        formulaire.setDescription(request.description());
        formulaire.setStatut(StatutFormulaire.BROUILLON);
        formulaire.setDateDebut(request.dateDebut());
        formulaire.setDateFin(request.dateFin());

        FormulaireCollecte saved = formulaireRepository.save(formulaire);
        return toFormulaireResponse(saved);
    }

    public FormulaireResponse publierFormulaire(UUID id) {
        FormulaireCollecte formulaire = formulaireRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Formulaire introuvable avec l'identifiant : " + id));

        if (formulaire.getStatut() != StatutFormulaire.BROUILLON) {
            throw new BusinessException("Seul un formulaire en BROUILLON peut être publié. Statut actuel : " + formulaire.getStatut());
        }

        long nombreChamps = champRepository.countByFormulaireId(id);
        if (nombreChamps == 0) {
            throw new BusinessException("Impossible de publier un formulaire sans aucun champ défini");
        }

        formulaire.setStatut(StatutFormulaire.PUBLIE);
        FormulaireCollecte saved = formulaireRepository.save(formulaire);
        return toFormulaireResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FormulaireResponse> listerFormulaires(UUID projetId) {
        return formulaireRepository.findByProjetId(projetId).stream()
            .map(this::toFormulaireResponse)
            .toList();
    }

    public DonneeCollecteeResponse soumettreReponse(DonneeCollecteeRequest request, UUID collecteurId) {
        FormulaireCollecte formulaire = formulaireRepository.findById(request.formulaireId())
            .orElseThrow(() -> new ResourceNotFoundException("Formulaire introuvable avec l'identifiant : " + request.formulaireId()));

        if (formulaire.getStatut() != StatutFormulaire.PUBLIE) {
            throw new BusinessException("Les réponses ne peuvent être soumises que pour un formulaire publié. Statut actuel : " + formulaire.getStatut());
        }

        DonneeCollectee donnee = new DonneeCollectee();
        donnee.setFormulaireId(request.formulaireId());
        donnee.setCollecteurId(collecteurId);
        donnee.setDateCollecte(request.dateCollecte());
        donnee.setLocalisation(request.localisation());
        donnee.setDonnees(request.donnees());
        donnee.setValide(false);

        DonneeCollectee saved = donneeRepository.save(donnee);
        return toDonneeResponse(saved);
    }

    public DonneeCollecteeResponse validerReponse(UUID donneeId, UUID validateurId) {
        DonneeCollectee donnee = donneeRepository.findById(donneeId)
            .orElseThrow(() -> new ResourceNotFoundException("Donnée collectée introuvable avec l'identifiant : " + donneeId));

        if (Boolean.TRUE.equals(donnee.getValide())) {
            throw new BusinessException("Cette donnée a déjà été validée");
        }

        donnee.setValide(true);
        donnee.setValideParId(validateurId);

        DonneeCollectee saved = donneeRepository.save(donnee);
        return toDonneeResponse(saved);
    }

    private FormulaireResponse toFormulaireResponse(FormulaireCollecte f) {
        return new FormulaireResponse(
            f.getId(),
            f.getProjetId(),
            f.getTitre(),
            f.getDescription(),
            f.getStatut(),
            f.getDateDebut(),
            f.getDateFin(),
            f.getCreatedAt()
        );
    }

    private DonneeCollecteeResponse toDonneeResponse(DonneeCollectee d) {
        return new DonneeCollecteeResponse(
            d.getId(),
            d.getFormulaireId(),
            d.getCollecteurId(),
            d.getDateCollecte(),
            d.getLocalisation(),
            d.getDonnees(),
            d.getValide(),
            d.getCreatedAt()
        );
    }
}
