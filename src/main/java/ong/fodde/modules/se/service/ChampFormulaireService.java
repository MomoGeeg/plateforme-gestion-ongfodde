package ong.fodde.modules.se.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.se.dto.ChampFormulaireRequest;
import ong.fodde.modules.se.dto.ChampFormulaireResponse;
import ong.fodde.modules.se.entity.ChampFormulaire;
import ong.fodde.modules.se.repository.ChampFormulaireRepository;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class ChampFormulaireService {

    private final ChampFormulaireRepository champFormulaireRepository;

    public ChampFormulaireService(ChampFormulaireRepository champFormulaireRepository) {
        this.champFormulaireRepository = champFormulaireRepository;
    }

    public ChampFormulaireResponse ajouterChamp(UUID formulaireId, ChampFormulaireRequest request) {
        ChampFormulaire champ = new ChampFormulaire();
        champ.setFormulaireId(formulaireId);
        champ.setLibelle(request.libelle());
        champ.setTypeChamp(request.typeChamp());
        champ.setEstObligatoire(request.obligatoire() != null ? request.obligatoire() : true);
        champ.setOrdre(request.ordre() != null ? request.ordre() : 1);
        champ.setOptions(request.options());

        ChampFormulaire saved = champFormulaireRepository.save(champ);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ChampFormulaireResponse> listerChamps(UUID formulaireId) {
        return champFormulaireRepository.findByFormulaireId(formulaireId).stream()
                .map(this::toResponse)
                .toList();
    }

    public void supprimerChamp(UUID champId) {
        if (!champFormulaireRepository.existsById(champId)) {
            throw new ResourceNotFoundException("Champ de formulaire non trouvé avec l'id : " + champId);
        }
        champFormulaireRepository.deleteById(champId);
    }

    private ChampFormulaireResponse toResponse(ChampFormulaire c) {
        return new ChampFormulaireResponse(
                c.getId(),
                c.getFormulaireId(),
                c.getLibelle(),
                c.getTypeChamp(),
                c.getEstObligatoire(),
                c.getOrdre(),
                c.getOptions()
        );
    }
}
