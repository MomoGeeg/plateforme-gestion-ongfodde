package ong.fodde.modules.financier.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.dto.AllocationProjetRequest;
import ong.fodde.modules.financier.dto.AllocationProjetResponse;
import ong.fodde.modules.financier.dto.LigneBudgetaireRequest;
import ong.fodde.modules.financier.dto.LigneBudgetaireResponse;
import ong.fodde.modules.financier.entity.AllocationProjet;
import ong.fodde.modules.financier.entity.BudgetAnnuel;
import ong.fodde.modules.financier.entity.LigneBudgetaire;
import ong.fodde.modules.financier.entity.StatutAllocation;
import ong.fodde.modules.financier.entity.StatutBudgetAnnuel;
import ong.fodde.modules.financier.repository.AllocationProjetRepository;
import ong.fodde.modules.financier.repository.BudgetAnnuelRepository;
import ong.fodde.modules.financier.repository.LigneBudgetaireRepository;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class AllocationProjetService {

    private final AllocationProjetRepository allocationProjetRepository;
    private final BudgetAnnuelRepository budgetAnnuelRepository;
    private final LigneBudgetaireRepository ligneBudgetaireRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    public AllocationProjetService(AllocationProjetRepository allocationProjetRepository,
                                    BudgetAnnuelRepository budgetAnnuelRepository,
                                    LigneBudgetaireRepository ligneBudgetaireRepository,
                                    ProjetRepository projetRepository,
                                    UserRepository userRepository) {
        this.allocationProjetRepository = allocationProjetRepository;
        this.budgetAnnuelRepository = budgetAnnuelRepository;
        this.ligneBudgetaireRepository = ligneBudgetaireRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
    }

    // ── Création allocation ───────────────────────────────────────────────────

    public AllocationProjetResponse creerAllocation(AllocationProjetRequest request, UUID soumisParId) {
        BudgetAnnuel ba = budgetAnnuelRepository.findById(request.budgetAnnuelId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget annuel introuvable"));

        if (ba.getStatut() == StatutBudgetAnnuel.CLOTURE) {
            throw new BusinessException("Impossible d'allouer un budget clôturé");
        }

        allocationProjetRepository.findByBudgetAnnuelIdAndProjetId(
                request.budgetAnnuelId(), request.projetId()).ifPresent(a -> {
            if (a.getStatut() != StatutAllocation.REJETEE) {
                throw new BusinessException("Une allocation existe déjà pour ce projet dans cet exercice");
            }
        });

        // Vérifier que le montant ne dépasse pas le disponible
        BigDecimal totalAlloue = allocationProjetRepository.findByBudgetAnnuelId(request.budgetAnnuelId())
                .stream()
                .filter(a -> a.getStatut() != StatutAllocation.REJETEE)
                .map(AllocationProjet::getMontantAlloue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAlloue.add(request.montantAlloue()).compareTo(ba.getMontantTotal()) > 0) {
            BigDecimal disponible = ba.getMontantTotal().subtract(totalAlloue);
            throw new BusinessException(
                "Montant disponible insuffisant. Disponible : " + disponible
                + " " + ba.getDevise() + ", demandé : " + request.montantAlloue());
        }

        AllocationProjet alloc = new AllocationProjet();
        alloc.setBudgetAnnuelId(request.budgetAnnuelId());
        alloc.setProjetId(request.projetId());
        alloc.setMontantAlloue(request.montantAlloue());
        alloc.setSoumisParId(soumisParId);
        alloc.setStatut(StatutAllocation.BROUILLON);

        return toResponse(allocationProjetRepository.save(alloc), Collections.emptyList());
    }

    // ── Lignes budgétaires ────────────────────────────────────────────────────

    public LigneBudgetaireResponse ajouterLigne(LigneBudgetaireRequest request) {
        AllocationProjet alloc = allocationProjetRepository.findById(request.allocationProjetId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Allocation introuvable : " + request.allocationProjetId()));

        if (alloc.getStatut() != StatutAllocation.BROUILLON
                && alloc.getStatut() != StatutAllocation.REJETEE) {
            throw new BusinessException(
                "Les lignes ne peuvent être modifiées qu'en statut BROUILLON ou REJETEE");
        }

        ligneBudgetaireRepository.findByAllocationProjetIdAndCode(
                request.allocationProjetId(), request.code()).ifPresent(l -> {
            throw new BusinessException(
                "Une ligne avec le code '" + request.code() + "' existe déjà dans cette allocation");
        });

        BigDecimal totalExistant = ligneBudgetaireRepository
                .findByAllocationProjetId(request.allocationProjetId())
                .stream()
                .map(LigneBudgetaire::getMontantPrevu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalExistant.add(request.montantPrevu()).compareTo(alloc.getMontantAlloue()) > 0) {
            throw new BusinessException(
                "Le total des lignes (" + totalExistant.add(request.montantPrevu())
                + ") dépasse le montant alloué (" + alloc.getMontantAlloue() + ")");
        }

        LigneBudgetaire ligne = new LigneBudgetaire();
        ligne.setAllocationProjetId(request.allocationProjetId());
        ligne.setCode(request.code());
        ligne.setIntitule(request.intitule());
        ligne.setCategorie(request.categorie());
        ligne.setMontantPrevu(request.montantPrevu());

        return toLigneResponse(ligneBudgetaireRepository.save(ligne));
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AllocationProjetResponse> listerParBudgetAnnuel(UUID budgetAnnuelId) {
        return allocationProjetRepository.findByBudgetAnnuelId(budgetAnnuelId)
                .stream()
                .map(a -> {
                    List<LigneBudgetaireResponse> lignes = ligneBudgetaireRepository
                            .findByAllocationProjetId(a.getId())
                            .stream().map(this::toLigneResponse).toList();
                    return toResponse(a, lignes);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LigneBudgetaireResponse> getLignesByProjetId(UUID projetId) {
        return allocationProjetRepository.findByProjetId(projetId)
                .stream()
                .flatMap(alloc -> ligneBudgetaireRepository.findByAllocationProjetId(alloc.getId())
                        .stream().map(this::toLigneResponse))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AllocationProjetResponse> listerEnValidation() {
        return allocationProjetRepository.findByStatut(StatutAllocation.EN_VALIDATION)
                .stream()
                .map(a -> {
                    List<LigneBudgetaireResponse> lignes = ligneBudgetaireRepository
                            .findByAllocationProjetId(a.getId())
                            .stream().map(this::toLigneResponse).toList();
                    return toResponse(a, lignes);
                })
                .toList();
    }

    // ── Workflow validation ───────────────────────────────────────────────────

    public AllocationProjetResponse soumettreValidation(UUID id) {
        AllocationProjet alloc = allocationProjetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation introuvable : " + id));

        if (alloc.getStatut() != StatutAllocation.BROUILLON
                && alloc.getStatut() != StatutAllocation.REJETEE) {
            throw new BusinessException(
                "Seule une allocation BROUILLON ou REJETEE peut être soumise");
        }

        List<LigneBudgetaire> lignes = ligneBudgetaireRepository.findByAllocationProjetId(id);
        if (lignes.isEmpty()) {
            throw new BusinessException(
                "Ajoutez au moins une ligne budgétaire avant de soumettre");
        }

        alloc.setStatut(StatutAllocation.EN_VALIDATION);
        alloc.setMotifRejet(null);

        List<LigneBudgetaireResponse> lignesResp = lignes.stream().map(this::toLigneResponse).toList();
        return toResponse(allocationProjetRepository.save(alloc), lignesResp);
    }

    public AllocationProjetResponse validerAllocation(UUID id, UUID validateurId) {
        AllocationProjet alloc = allocationProjetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation introuvable : " + id));

        if (alloc.getStatut() != StatutAllocation.EN_VALIDATION) {
            throw new BusinessException("Seule une allocation EN_VALIDATION peut être validée");
        }

        alloc.setStatut(StatutAllocation.ACTIVE);
        alloc.setValideParId(validateurId);
        alloc.setDateValidation(LocalDate.now());
        alloc.setMotifRejet(null);

        List<LigneBudgetaireResponse> lignes = ligneBudgetaireRepository
                .findByAllocationProjetId(id).stream().map(this::toLigneResponse).toList();
        return toResponse(allocationProjetRepository.save(alloc), lignes);
    }

    public AllocationProjetResponse rejeterAllocation(UUID id, String motif) {
        AllocationProjet alloc = allocationProjetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation introuvable : " + id));

        if (alloc.getStatut() != StatutAllocation.EN_VALIDATION) {
            throw new BusinessException("Seule une allocation EN_VALIDATION peut être rejetée");
        }

        alloc.setStatut(StatutAllocation.REJETEE);
        alloc.setMotifRejet(motif);

        List<LigneBudgetaireResponse> lignes = ligneBudgetaireRepository
                .findByAllocationProjetId(id).stream().map(this::toLigneResponse).toList();
        return toResponse(allocationProjetRepository.save(alloc), lignes);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    AllocationProjetResponse toResponse(AllocationProjet alloc, List<LigneBudgetaireResponse> lignes) {
        Projet projet = projetRepository.findById(alloc.getProjetId()).orElse(null);

        BudgetAnnuel ba = budgetAnnuelRepository.findById(alloc.getBudgetAnnuelId()).orElse(null);
        Integer exercice = ba != null ? ba.getExercice() : null;

        String soumisParNom = alloc.getSoumisParId() != null
                ? userRepository.findById(alloc.getSoumisParId())
                        .map(u -> u.getPrenom() + " " + u.getNom()).orElse(null)
                : null;

        String valideParNom = alloc.getValideParId() != null
                ? userRepository.findById(alloc.getValideParId())
                        .map(u -> u.getPrenom() + " " + u.getNom()).orElse(null)
                : null;

        BigDecimal montantPrevu = lignes.stream()
                .map(LigneBudgetaireResponse::montantPrevu)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantDepense = lignes.stream()
                .map(LigneBudgetaireResponse::montantDepense)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal soldeDisponible = alloc.getMontantAlloue().subtract(montantDepense);

        return new AllocationProjetResponse(
                alloc.getId(),
                alloc.getBudgetAnnuelId(),
                exercice,
                alloc.getProjetId(),
                projet != null ? projet.getCode() : null,
                projet != null ? projet.getTitre() : null,
                null, // bailleur : à ajouter sur l'entité Projet si nécessaire
                alloc.getMontantAlloue(),
                alloc.getStatut(),
                alloc.getMotifRejet(),
                soumisParNom,
                valideParNom,
                alloc.getDateValidation(),
                lignes,
                montantPrevu,
                montantDepense,
                soldeDisponible,
                alloc.getCreatedAt());
    }

    LigneBudgetaireResponse toLigneResponse(LigneBudgetaire ligne) {
        BigDecimal montantPrevu = ligne.getMontantPrevu() != null ? ligne.getMontantPrevu() : BigDecimal.ZERO;
        BigDecimal montantDepense = ligne.getMontantDepense() != null ? ligne.getMontantDepense() : BigDecimal.ZERO;
        BigDecimal soldeDisponible = montantPrevu.subtract(montantDepense);

        double tauxConsommation = 0.0;
        if (montantPrevu.compareTo(BigDecimal.ZERO) > 0) {
            tauxConsommation = montantDepense
                    .multiply(BigDecimal.valueOf(100))
                    .divide(montantPrevu, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new LigneBudgetaireResponse(
                ligne.getId(),
                ligne.getAllocationProjetId(),
                ligne.getCode(),
                ligne.getIntitule(),
                ligne.getCategorie(),
                montantPrevu,
                ligne.getMontantEngage(),
                montantDepense,
                ligne.getEstGelee(),
                soldeDisponible,
                tauxConsommation);
    }
}
