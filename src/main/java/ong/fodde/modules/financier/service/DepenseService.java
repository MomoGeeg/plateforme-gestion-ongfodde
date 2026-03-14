package ong.fodde.modules.financier.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.dto.DepenseRequest;
import ong.fodde.modules.financier.dto.DepenseResponse;
import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.LigneBudgetaire;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.financier.repository.DepenseRepository;
import ong.fodde.modules.financier.repository.LigneBudgetaireRepository;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final LigneBudgetaireRepository ligneBudgetaireRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    /** Transitions de statut autorisées pour les dépenses */
    private static final Map<StatutDepense, Set<StatutDepense>> TRANSITIONS = Map.of(
        StatutDepense.BROUILLON,          Set.of(StatutDepense.SOUMISE, StatutDepense.ANNULEE),
        StatutDepense.SOUMISE,            Set.of(StatutDepense.VERIFIEE, StatutDepense.REJETEE),
        StatutDepense.VERIFIEE,           Set.of(StatutDepense.VALIDEE, StatutDepense.EN_APPROBATION_SE, StatutDepense.REJETEE),
        StatutDepense.EN_APPROBATION_SE,  Set.of(StatutDepense.VALIDEE, StatutDepense.REJETEE),
        StatutDepense.VALIDEE,            Set.of(),
        StatutDepense.REJETEE,            Set.of(StatutDepense.BROUILLON),
        StatutDepense.ANNULEE,            Set.of()
    );

    private long compteurDepense = 0;
    private int anneeCompteur = 0;

    public DepenseService(DepenseRepository depenseRepository,
                           LigneBudgetaireRepository ligneBudgetaireRepository,
                           ProjetRepository projetRepository,
                           UserRepository userRepository) {
        this.depenseRepository = depenseRepository;
        this.ligneBudgetaireRepository = ligneBudgetaireRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
    }

    public DepenseResponse soumettreDepense(DepenseRequest request, UUID userId) {
        LigneBudgetaire ligne = ligneBudgetaireRepository.findById(request.ligneBudgetaireId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Ligne budgétaire introuvable avec l'identifiant : " + request.ligneBudgetaireId()));

        // Vérifier que la ligne n'est pas gelée
        if (Boolean.TRUE.equals(ligne.getEstGelee())) {
            throw new BusinessException("La ligne budgétaire '" + ligne.getCode() + "' est gelée. Aucune dépense ne peut être enregistrée.");
        }

        // Vérifier le statut du projet
        Projet projet = projetRepository.findById(request.projetId())
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable"));
        if (projet.getStatut() == StatutProjet.SUSPENDU) {
            throw new BusinessException("Impossible d'enregistrer une dépense sur un projet suspendu");
        }
        if (projet.getStatut() == StatutProjet.CLOTURE) {
            throw new BusinessException("Impossible d'enregistrer une dépense sur un projet clôturé");
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
    public List<DepenseResponse> listerToutesDepenses(StatutDepense statut) {
        return depenseRepository.findAll().stream()
            .filter(d -> statut == null || d.getStatut() == statut)
            .map(this::toResponse)
            .toList();
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

        // Mise à jour du montant dépensé sur la ligne budgétaire lors de la validation
        if (nouveauStatut == StatutDepense.VALIDEE) {
            LigneBudgetaire ligne = ligneBudgetaireRepository.findById(depense.getLigneBudgetaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Ligne budgétaire introuvable"));
            BigDecimal montantDepenseActuel = ligne.getMontantDepense() != null ? ligne.getMontantDepense() : BigDecimal.ZERO;
            ligne.setMontantDepense(montantDepenseActuel.add(depense.getMontant()));
            ligneBudgetaireRepository.save(ligne);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepenseResponse> mesDepenses(UUID userId) {
        return depenseRepository.findBySaisiParId(userId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal totalDepensesMoisCourant() {
        LocalDate debut = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        BigDecimal total = depenseRepository.sumByPeriode(debut, fin);
        return total != null ? total : BigDecimal.ZERO;
    }

    private String genererNumeroPiece() {
        int annee = LocalDate.now().getYear();
        // Réinitialiser depuis la DB si premier appel ou changement d'année
        if (anneeCompteur != annee) {
            compteurDepense = depenseRepository.findMaxSequenceForYear(annee);
            anneeCompteur = annee;
        }
        compteurDepense++;
        return String.format("DEP-%d-%06d", annee, compteurDepense);
    }

    @Transactional(readOnly = true)
    public DepenseResponse getDepenseById(UUID id) {
        Depense depense = depenseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable : " + id));
        return toResponse(depense);
    }

    public DepenseResponse annulerDepense(UUID id, String motif, UUID userId) {
        Depense depense = depenseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Dépense introuvable : " + id));

        if (depense.getStatut() == StatutDepense.ANNULEE) {
            throw new BusinessException("La dépense est déjà annulée");
        }

        if (depense.getStatut() == StatutDepense.VALIDEE) {
            LigneBudgetaire ligne = ligneBudgetaireRepository.findById(depense.getLigneBudgetaireId())
                .orElseThrow(() -> new ResourceNotFoundException("Ligne budgétaire introuvable"));
            BigDecimal actuel = ligne.getMontantDepense() != null ? ligne.getMontantDepense() : BigDecimal.ZERO;
            ligne.setMontantDepense(actuel.subtract(depense.getMontant()));
            ligneBudgetaireRepository.save(ligne);
        }

        depense.setStatut(StatutDepense.ANNULEE);
        depense.setMotifRejet(motif);
        return toResponse(depenseRepository.save(depense));
    }

    private DepenseResponse toResponse(Depense depense) {
        // Enrichissement : ligne budgétaire
        LigneBudgetaire ligne = ligneBudgetaireRepository.findById(depense.getLigneBudgetaireId()).orElse(null);
        String ligneCode     = ligne != null ? ligne.getCode()    : null;
        String ligneIntitule = ligne != null ? ligne.getIntitule() : null;

        // Enrichissement : projet
        Projet projet = projetRepository.findById(depense.getProjetId()).orElse(null);
        String projetCode  = projet != null ? projet.getCode()  : null;
        String projetTitre = projet != null ? projet.getTitre() : null;

        // Enrichissement : auteur
        String saisiParNom = userRepository.findById(depense.getSaisiParId())
            .map(u -> u.getPrenom() + " " + u.getNom())
            .orElse(null);

        // Jours d'attente depuis la date de dépense
        long joursAttente = depense.getDateDepense() != null
            ? ChronoUnit.DAYS.between(depense.getDateDepense(), LocalDate.now())
            : 0;

        return new DepenseResponse(
            depense.getId(),
            depense.getNumeroPiece(),
            depense.getLigneBudgetaireId(),
            ligneCode,
            ligneIntitule,
            depense.getProjetId(),
            projetCode,
            projetTitre,
            depense.getLibelle(),
            depense.getMontant(),
            depense.getDevise(),
            depense.getModePaiement(),
            depense.getBeneficiaire(),
            depense.getDateDepense(),
            depense.getStatut(),
            depense.getSaisiParId(),
            saisiParNom,
            depense.getMotifRejet(),
            joursAttente,
            depense.getCreatedAt()
        );
    }
}
