package ong.fodde.modules.financier.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.dto.BudgetAnnuelRequest;
import ong.fodde.modules.financier.dto.BudgetAnnuelResponse;
import ong.fodde.modules.financier.entity.AllocationProjet;
import ong.fodde.modules.financier.entity.BudgetAnnuel;
import ong.fodde.modules.financier.entity.StatutBudgetAnnuel;
import ong.fodde.modules.financier.repository.AllocationProjetRepository;
import ong.fodde.modules.financier.repository.BudgetAnnuelRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class BudgetAnnuelService {

    private final BudgetAnnuelRepository budgetAnnuelRepository;
    private final AllocationProjetRepository allocationProjetRepository;
    private final UserRepository userRepository;

    public BudgetAnnuelService(BudgetAnnuelRepository budgetAnnuelRepository,
                                AllocationProjetRepository allocationProjetRepository,
                                UserRepository userRepository) {
        this.budgetAnnuelRepository = budgetAnnuelRepository;
        this.allocationProjetRepository = allocationProjetRepository;
        this.userRepository = userRepository;
    }

    // ── Création ──────────────────────────────────────────────────────────────

    public BudgetAnnuelResponse creerBudgetAnnuel(BudgetAnnuelRequest request, UUID saisiParId) {
        budgetAnnuelRepository.findByExercice(request.exercice()).ifPresent(b -> {
            throw new BusinessException(
                "Un budget annuel existe déjà pour l'exercice " + request.exercice()
                + " (statut : " + b.getStatut() + ")");
        });

        BudgetAnnuel ba = new BudgetAnnuel();
        ba.setExercice(request.exercice());
        ba.setIntitule(request.intitule() != null ? request.intitule()
                : "Budget exercice " + request.exercice());
        ba.setMontantTotal(request.montantTotal());
        ba.setDevise(request.devise() != null ? request.devise() : "XOF");
        ba.setDateVote(request.dateVote());
        ba.setStatut(StatutBudgetAnnuel.BROUILLON);
        ba.setSaisiParId(saisiParId);

        return toResponse(budgetAnnuelRepository.save(ba));
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BudgetAnnuelResponse> listerTous() {
        return budgetAnnuelRepository.findAllByOrderByExerciceDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BudgetAnnuelResponse obtenirParId(UUID id) {
        return toResponse(budgetAnnuelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget annuel introuvable : " + id)));
    }

    // ── Workflow statut ───────────────────────────────────────────────────────

    public BudgetAnnuelResponse activerBudget(UUID id) {
        BudgetAnnuel ba = budgetAnnuelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget annuel introuvable : " + id));

        if (ba.getStatut() != StatutBudgetAnnuel.BROUILLON) {
            throw new BusinessException("Seul un budget BROUILLON peut être activé (statut actuel : " + ba.getStatut() + ")");
        }

        ba.setStatut(StatutBudgetAnnuel.ACTIF);
        return toResponse(budgetAnnuelRepository.save(ba));
    }

    public BudgetAnnuelResponse cloturerBudget(UUID id) {
        BudgetAnnuel ba = budgetAnnuelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget annuel introuvable : " + id));

        if (ba.getStatut() != StatutBudgetAnnuel.ACTIF) {
            throw new BusinessException("Seul un budget ACTIF peut être clôturé");
        }

        ba.setStatut(StatutBudgetAnnuel.CLOTURE);
        return toResponse(budgetAnnuelRepository.save(ba));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    BudgetAnnuelResponse toResponse(BudgetAnnuel ba) {
        List<AllocationProjet> allocations = allocationProjetRepository.findByBudgetAnnuelId(ba.getId());

        BigDecimal montantAlloue = allocations.stream()
                .map(AllocationProjet::getMontantAlloue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantDisponible = ba.getMontantTotal().subtract(montantAlloue);

        String saisiPar = ba.getSaisiParId() != null
                ? userRepository.findById(ba.getSaisiParId())
                        .map(u -> u.getPrenom() + " " + u.getNom()).orElse(null)
                : null;

        return new BudgetAnnuelResponse(
                ba.getId(),
                ba.getExercice(),
                ba.getIntitule(),
                ba.getMontantTotal(),
                ba.getDevise(),
                ba.getDateVote(),
                ba.getStatut(),
                saisiPar,
                montantAlloue,
                montantDisponible,
                allocations.size(),
                ba.getCreatedAt());
    }
}
