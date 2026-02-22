package ong.fodde.modules.financier.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.financier.dto.BudgetRequest;
import ong.fodde.modules.financier.dto.BudgetResponse;
import ong.fodde.modules.financier.dto.LigneBudgetaireRequest;
import ong.fodde.modules.financier.dto.LigneBudgetaireResponse;
import ong.fodde.modules.financier.entity.Budget;
import ong.fodde.modules.financier.entity.LigneBudgetaire;
import ong.fodde.modules.financier.entity.StatutBudget;
import ong.fodde.modules.financier.repository.BudgetRepository;
import ong.fodde.modules.financier.repository.LigneBudgetaireRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final LigneBudgetaireRepository ligneBudgetaireRepository;

    public BudgetService(BudgetRepository budgetRepository, LigneBudgetaireRepository ligneBudgetaireRepository) {
        this.budgetRepository = budgetRepository;
        this.ligneBudgetaireRepository = ligneBudgetaireRepository;
    }

    public BudgetResponse creerBudget(BudgetRequest request) {
        budgetRepository.findByProjetId(request.projetId())
            .filter(b -> b.getStatut() == StatutBudget.ACTIF)
            .ifPresent(b -> {
                throw new BusinessException("Un budget actif existe déjà pour ce projet");
            });

        Budget budget = new Budget();
        budget.setProjetId(request.projetId());
        budget.setMontantTotal(request.montantTotal());
        budget.setDevise(request.devise());
        budget.setStatut(StatutBudget.ACTIF);
        budget.setVersion(1);

        Budget saved = budgetRepository.save(budget);
        return toBudgetResponse(saved, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse obtenirBudgetParProjet(UUID projetId) {
        Budget budget = budgetRepository.findByProjetId(projetId)
            .orElseThrow(() -> new ResourceNotFoundException("Aucun budget trouvé pour le projet : " + projetId));

        List<LigneBudgetaireResponse> lignes = ligneBudgetaireRepository.findByBudgetId(budget.getId())
            .stream()
            .map(this::toLigneResponse)
            .toList();

        return toBudgetResponse(budget, lignes);
    }

    public LigneBudgetaireResponse ajouterLigne(LigneBudgetaireRequest request) {
        Budget budget = budgetRepository.findById(request.budgetId())
            .orElseThrow(() -> new ResourceNotFoundException("Budget introuvable avec l'identifiant : " + request.budgetId()));

        // Vérifier que le code n'existe pas déjà dans ce budget
        ligneBudgetaireRepository.findByBudgetIdAndCode(request.budgetId(), request.code())
            .ifPresent(l -> {
                throw new BusinessException("Une ligne budgétaire avec le code '" + request.code() + "' existe déjà dans ce budget");
            });

        // Vérifier que la somme des montants ne dépasse pas le budget total
        BigDecimal totalExistant = ligneBudgetaireRepository.findByBudgetId(request.budgetId())
            .stream()
            .map(LigneBudgetaire::getMontantPrevu)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal nouveauTotal = totalExistant.add(request.montantPrevu());
        if (nouveauTotal.compareTo(budget.getMontantTotal()) > 0) {
            throw new BusinessException(
                "Le montant total des lignes (" + nouveauTotal + ") dépasse le budget total (" + budget.getMontantTotal() + ")"
            );
        }

        LigneBudgetaire ligne = new LigneBudgetaire();
        ligne.setBudgetId(request.budgetId());
        ligne.setCode(request.code());
        ligne.setIntitule(request.intitule());
        ligne.setCategorie(request.categorie());
        ligne.setMontantPrevu(request.montantPrevu());

        LigneBudgetaire saved = ligneBudgetaireRepository.save(ligne);
        return toLigneResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LigneBudgetaireResponse> listerLignes(UUID budgetId) {
        budgetRepository.findById(budgetId)
            .orElseThrow(() -> new ResourceNotFoundException("Budget introuvable avec l'identifiant : " + budgetId));

        return ligneBudgetaireRepository.findByBudgetId(budgetId)
            .stream()
            .map(this::toLigneResponse)
            .toList();
    }

    private BudgetResponse toBudgetResponse(Budget budget, List<LigneBudgetaireResponse> lignes) {
        return new BudgetResponse(
            budget.getId(),
            budget.getProjetId(),
            budget.getVersion(),
            budget.getMontantTotal(),
            budget.getDevise(),
            budget.getStatut(),
            budget.getDateValidation(),
            budget.getCreatedAt(),
            lignes
        );
    }

    private LigneBudgetaireResponse toLigneResponse(LigneBudgetaire ligne) {
        BigDecimal montantPrevu = ligne.getMontantPrevu() != null ? ligne.getMontantPrevu() : BigDecimal.ZERO;
        BigDecimal montantDepense = ligne.getMontantDepense() != null ? ligne.getMontantDepense() : BigDecimal.ZERO;

        BigDecimal soldeDisponible = montantPrevu.subtract(montantDepense);

        Double tauxConsommation = 0.0;
        if (montantPrevu.compareTo(BigDecimal.ZERO) > 0) {
            tauxConsommation = montantDepense
                .multiply(BigDecimal.valueOf(100))
                .divide(montantPrevu, 2, RoundingMode.HALF_UP)
                .doubleValue();
        }

        return new LigneBudgetaireResponse(
            ligne.getId(),
            ligne.getBudgetId(),
            ligne.getCode(),
            ligne.getIntitule(),
            ligne.getCategorie(),
            montantPrevu,
            ligne.getMontantEngage(),
            montantDepense,
            ligne.getEstGelee(),
            soldeDisponible,
            tauxConsommation
        );
    }
}
