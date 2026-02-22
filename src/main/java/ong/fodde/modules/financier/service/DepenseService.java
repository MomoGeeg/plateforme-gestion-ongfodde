package ong.fodde.modules.financier.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.financier.dto.DepenseRequest;
import ong.fodde.modules.financier.dto.DepenseResponse;
import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.LigneBudgetaire;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.financier.repository.DepenseRepository;
import ong.fodde.modules.financier.repository.LigneBudgetaireRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final LigneBudgetaireRepository ligneBudgetaireRepository;

    /** Transitions de statut autorisées pour les dépenses */
    private static final Map<StatutDepense, Set<StatutDepense>> TRANSITIONS = Map.of(
        StatutDepense.BROUILLON,          Set.of(StatutDepense.SOUMISE, StatutDepense.ANNULEE),
        StatutDepense.SOUMISE,            Set.of(StatutDepense.VERIFIEE, StatutDepense.REJETEE),
        StatutDepense.VERIFIEE,           Set.of(StatutDepense.EN_APPROBATION_SE, StatutDepense.REJETEE),
        StatutDepense.EN_APPROBATION_SE,  Set.of(StatutDepense.VALIDEE, StatutDepense.REJETEE),
        StatutDepense.VALIDEE,            Set.of(),
        StatutDepense.REJETEE,            Set.of(StatutDepense.BROUILLON),
        StatutDepense.ANNULEE,            Set.of()
    );

    private long compteurDepense = 0;

    public DepenseService(DepenseRepository depenseRepository, LigneBudgetaireRepository ligneBudgetaireRepository) {
        this.depenseRepository = depenseRepository;
        this.ligneBudgetaireRepository = ligneBudgetaireRepository;
    }

    public DepenseResponse soumettreDepense(DepenseRequest request, UUID userId) {
        LigneBudgetaire ligne = ligneBudgetaireRepository.findById(request.ligneBudgetaireId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Ligne budgétaire introuvable avec l'identifiant : " + request.ligneBudgetaireId()));

        // Vérifier que la ligne n'est pas gelée
        if (Boolean.TRUE.equals(ligne.getEstGelee())) {
            throw new BusinessException("La ligne budgétaire '" + ligne.getCode() + "' est gelée. Aucune dépense ne peut être enregistrée.");
        }

        // Vérifier le solde disponible
        BigDecimal montantPrevu = ligne.getMontantPrevu() != null ? ligne.getMontantPrevu() : BigDecimal.ZERO;
        BigDecimal montantDepense = ligne.getMontantDepense() != null ? ligne.getMontantDepense() : BigDecimal.ZERO;
        BigDecimal soldeDisponible = montantPrevu.subtract(montantDepense);

        if (request.montant().compareTo(soldeDisponible) > 0) {
            throw new BusinessException(
                "Solde insuffisant sur la ligne '" + ligne.getCode() + "'. Disponible : " + soldeDisponible + ", demandé : " + request.montant()
            );
        }

        // Générer le numéro de pièce
        String numeroPiece = genererNumeroPiece();

        Depense depense = new Depense();
        depense.setNumeroPiece(numeroPiece);
        depense.setLigneBudgetaireId(request.ligneBudgetaireId());
        depense.setProjetId(request.projetId());
        depense.setLibelle(request.libelle());
        depense.setMontant(request.montant());
        depense.setDevise(request.devise());
        depense.setModePaiement(request.modePaiement());
        depense.setBeneficiaire(request.beneficiaire());
        depense.setDateDepense(request.dateDepense());
        depense.setStatut(StatutDepense.BROUILLON);
        depense.setSaisiParId(userId);

        Depense saved = depenseRepository.save(depense);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepenseResponse> listerDepenses(UUID projetId) {
        return depenseRepository.findByProjetId(projetId).stream()
            .map(this::toResponse)
            .toList();
    }

    public DepenseResponse changerStatut(UUID id, StatutDepense nouveauStatut, UUID userId, String motif) {
        Depense depense = depenseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable avec l'identifiant : " + id));

        StatutDepense statutActuel = depense.getStatut();
        Set<StatutDepense> statutsAutorises = TRANSITIONS.getOrDefault(statutActuel, Set.of());

        if (!statutsAutorises.contains(nouveauStatut)) {
            throw new BusinessException(
                "Transition de statut invalide : " + statutActuel + " → " + nouveauStatut
            );
        }

        // Motif obligatoire pour un rejet
        if (nouveauStatut == StatutDepense.REJETEE && (motif == null || motif.isBlank())) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }

        depense.setStatut(nouveauStatut);

        if (nouveauStatut == StatutDepense.REJETEE) {
            depense.setMotifRejet(motif);
        }
        if (nouveauStatut == StatutDepense.VERIFIEE) {
            depense.setVerifiePar(userId);
        }
        if (nouveauStatut == StatutDepense.VALIDEE) {
            depense.setValidePar(userId);
        }

        Depense saved = depenseRepository.save(depense);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepenseResponse> mesDepenses(UUID userId) {
        return depenseRepository.findBySaisiParId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    private String genererNumeroPiece() {
        int annee = LocalDate.now().getYear();
        compteurDepense++;
        return String.format("DEP-%d-%06d", annee, compteurDepense);
    }

    private DepenseResponse toResponse(Depense depense) {
        return new DepenseResponse(
            depense.getId(),
            depense.getNumeroPiece(),
            depense.getLigneBudgetaireId(),
            depense.getProjetId(),
            depense.getLibelle(),
            depense.getMontant(),
            depense.getDevise(),
            depense.getModePaiement(),
            depense.getBeneficiaire(),
            depense.getDateDepense(),
            depense.getStatut(),
            depense.getSaisiParId(),
            depense.getCreatedAt()
        );
    }
}
