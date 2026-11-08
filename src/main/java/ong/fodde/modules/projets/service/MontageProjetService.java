package ong.fodde.modules.projets.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.financier.entity.Financement;
import ong.fodde.modules.financier.repository.FinancementRepository;
import ong.fodde.modules.projets.dto.MontageDtos.BudgetRequest;
import ong.fodde.modules.projets.dto.MontageDtos.Controle;
import ong.fodde.modules.projets.dto.MontageDtos.FinancementResume;
import ong.fodde.modules.projets.dto.MontageDtos.IndicateurResume;
import ong.fodde.modules.projets.dto.MontageDtos.LigneBudgetRequest;
import ong.fodde.modules.projets.dto.MontageDtos.LigneBudgetResponse;
import ong.fodde.modules.projets.dto.MontageDtos.MembreResponse;
import ong.fodde.modules.projets.dto.MontageDtos.MontageResponse;
import ong.fodde.modules.projets.dto.MontageDtos.ObjectifRequest;
import ong.fodde.modules.projets.dto.MontageDtos.ObjectifResponse;
import ong.fodde.modules.projets.dto.MontageDtos.ResultatRequest;
import ong.fodde.modules.projets.dto.MontageDtos.ResultatResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.LigneBudgetProjet;
import ong.fodde.modules.projets.entity.ObjectifSpecifique;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.ResultatAttendu;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.BailleurRepository;
import ong.fodde.modules.projets.repository.LigneBudgetProjetRepository;
import ong.fodde.modules.projets.repository.ObjectifSpecifiqueRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.ResultatAttenduRepository;
import ong.fodde.modules.se.entity.Indicateur;
import ong.fodde.modules.se.repository.IndicateurRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Montage d'un projet selon le cadre logique :
 * objectif général → objectifs spécifiques → résultats attendus → activités ;
 * indicateurs par résultat ; budget prévisionnel ligne par ligne sur toute la durée.
 */
@Service
@Transactional
public class MontageProjetService {

    private final ProjetRepository projetRepository;
    private final ObjectifSpecifiqueRepository objectifRepository;
    private final ResultatAttenduRepository resultatRepository;
    private final ActiviteRepository activiteRepository;
    private final IndicateurRepository indicateurRepository;
    private final LigneBudgetProjetRepository budgetRepository;
    private final FinancementRepository financementRepository;
    private final BailleurRepository bailleurRepository;
    private final ActiviteService activiteService;
    private final DroitsProjet droits;
    private final ProjetMapper mapper;

    public MontageProjetService(ProjetRepository projetRepository,
                                ObjectifSpecifiqueRepository objectifRepository,
                                ResultatAttenduRepository resultatRepository,
                                ActiviteRepository activiteRepository,
                                IndicateurRepository indicateurRepository,
                                LigneBudgetProjetRepository budgetRepository,
                                FinancementRepository financementRepository,
                                BailleurRepository bailleurRepository,
                                ActiviteService activiteService,
                                DroitsProjet droits,
                                ProjetMapper mapper) {
        this.projetRepository = projetRepository;
        this.objectifRepository = objectifRepository;
        this.resultatRepository = resultatRepository;
        this.activiteRepository = activiteRepository;
        this.indicateurRepository = indicateurRepository;
        this.budgetRepository = budgetRepository;
        this.financementRepository = financementRepository;
        this.bailleurRepository = bailleurRepository;
        this.activiteService = activiteService;
        this.droits = droits;
        this.mapper = mapper;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Vue d'ensemble
    // ═════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public MontageResponse montage(UUID projetId, User user) {
        Projet p = projet(projetId);
        List<FinancementResume> financements = financementRepository.findByProjetId(projetId).stream()
                .map(f -> new FinancementResume(f.getId(),
                        bailleurRepository.findById(f.getBailleurId()).map(b -> b.getNom()).orElse(null),
                        f.getReferenceConvention(), f.getMontantAccorde()))
                .toList();
        BigDecimal totalFinancements = financements.stream().map(FinancementResume::montantAccorde)
                .filter(m -> m != null).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MembreResponse> equipe = p.getEquipe().stream()
                .sorted(Comparator.comparing(User::getNom))
                .map(u -> new MembreResponse(u.getId(), u.getPrenom() + " " + u.getNom(),
                        u.getRoles().stream().map(Role::getNom).sorted().toList(),
                        u.getId().equals(p.getChefProjetId())))
                .toList();
        return new MontageResponse(
                mapper.toResponse(p),
                objectifRepository.findByProjetIdOrderByOrdreAsc(projetId).stream()
                        .map(o -> new ObjectifResponse(o.getId(), o.getLibelle(), o.getOrdre())).toList(),
                resultatRepository.findByProjetIdOrderByOrdreAsc(projetId).stream()
                        .map(r -> new ResultatResponse(r.getId(), r.getObjectifId(), r.getLibelle(), r.getOrdre()))
                        .toList(),
                activiteService.listerActivitesProjet(projetId),
                indicateurs(projetId).stream()
                        .map(i -> new IndicateurResume(i.getId(), i.getCode(), i.getLibelle(), i.getResultatId(),
                                i.getUnite(), i.getValeurBaseline(), i.getValeurCible()))
                        .toList(),
                budgetRepository.findByProjetIdOrderByOrdreAsc(projetId).stream()
                        .map(l -> new LigneBudgetResponse(l.getId(), l.getCode(), l.getIntitule(), l.getCategorie(),
                                l.getMontant(), l.getFinancementId()))
                        .toList(),
                financements,
                totalFinancements,
                equipe,
                controles(p),
                droits.peutMonter(p, user));
    }

    /**
     * Ce qui doit être prêt avant la soumission. Les contrôles non bloquants informent seulement
     * (ex. financements encore incomplets : une convention peut être signée après la validation).
     */
    @Transactional(readOnly = true)
    public List<Controle> controles(Projet p) {
        List<ResultatAttendu> resultats = resultatRepository.findByProjetIdOrderByOrdreAsc(p.getId());
        List<Activite> activites = activiteRepository.findByProjetId(p.getId());
        List<Indicateur> indicateurs = indicateurs(p.getId());
        BigDecimal budget = total(p.getId());
        Set<UUID> resultatsAvecActivite = activites.stream().map(Activite::getResultatId)
                .filter(id -> id != null).collect(Collectors.toSet());
        boolean datesOk = activites.stream().allMatch(a ->
                !a.getDatePrevuDebut().isBefore(p.getDateDebut()) && !a.getDatePrevuFin().isAfter(p.getDateFin()));
        BigDecimal financements = financementRepository.findByProjetId(p.getId()).stream()
                .map(Financement::getMontantAccorde).filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Controle> c = new ArrayList<>();
        c.add(new Controle("Objectif général renseigné", estRempli(p.getObjectifGeneral()), true));
        c.add(new Controle("Chef de projet désigné", p.getChefProjetId() != null, true));
        c.add(new Controle("Au moins un résultat attendu", !resultats.isEmpty(), true));
        c.add(new Controle("Chaque résultat a au moins une activité",
                !resultats.isEmpty() && resultats.stream().allMatch(r -> resultatsAvecActivite.contains(r.getId())), true));
        c.add(new Controle("Au moins un indicateur", !indicateurs.isEmpty(), true));
        c.add(new Controle("Activités comprises dans les dates du projet", datesOk, true));
        c.add(new Controle("Budget prévisionnel saisi", budget.signum() > 0, true));
        c.add(new Controle("Budget couvert par les conventions des bailleurs",
                budget.signum() > 0 && financements.compareTo(budget) >= 0, false));
        return c;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Objectifs spécifiques
    // ═════════════════════════════════════════════════════════════════════

    public ObjectifResponse ajouterObjectif(UUID projetId, ObjectifRequest req, User user) {
        Projet p = projetModifiable(projetId, user);
        ObjectifSpecifique o = new ObjectifSpecifique();
        o.setProjetId(p.getId());
        o.setLibelle(req.libelle().trim());
        o.setOrdre(req.ordre() != null ? req.ordre()
                : objectifRepository.findByProjetIdOrderByOrdreAsc(projetId).size() + 1);
        o = objectifRepository.save(o);
        return new ObjectifResponse(o.getId(), o.getLibelle(), o.getOrdre());
    }

    public ObjectifResponse modifierObjectif(UUID projetId, UUID id, ObjectifRequest req, User user) {
        projetModifiable(projetId, user);
        ObjectifSpecifique o = objectifRepository.findById(id).filter(x -> x.getProjetId().equals(projetId))
                .orElseThrow(() -> new ResourceNotFoundException("Objectif introuvable"));
        o.setLibelle(req.libelle().trim());
        if (req.ordre() != null) {
            o.setOrdre(req.ordre());
        }
        return new ObjectifResponse(o.getId(), o.getLibelle(), o.getOrdre());
    }

    /** Les résultats rattachés restent, sans objectif. */
    public void supprimerObjectif(UUID projetId, UUID id, User user) {
        projetModifiable(projetId, user);
        ObjectifSpecifique o = objectifRepository.findById(id).filter(x -> x.getProjetId().equals(projetId))
                .orElseThrow(() -> new ResourceNotFoundException("Objectif introuvable"));
        resultatRepository.findByProjetIdOrderByOrdreAsc(projetId).stream()
                .filter(r -> id.equals(r.getObjectifId()))
                .forEach(r -> r.setObjectifId(null));
        objectifRepository.delete(o);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Résultats attendus
    // ═════════════════════════════════════════════════════════════════════

    public ResultatResponse ajouterResultat(UUID projetId, ResultatRequest req, User user) {
        projetModifiable(projetId, user);
        verifierObjectif(projetId, req.objectifId());
        ResultatAttendu r = new ResultatAttendu();
        r.setProjetId(projetId);
        r.setObjectifId(req.objectifId());
        r.setLibelle(req.libelle().trim());
        r.setOrdre(req.ordre() != null ? req.ordre()
                : resultatRepository.findByProjetIdOrderByOrdreAsc(projetId).size() + 1);
        r = resultatRepository.save(r);
        return new ResultatResponse(r.getId(), r.getObjectifId(), r.getLibelle(), r.getOrdre());
    }

    public ResultatResponse modifierResultat(UUID projetId, UUID id, ResultatRequest req, User user) {
        projetModifiable(projetId, user);
        verifierObjectif(projetId, req.objectifId());
        ResultatAttendu r = resultat(projetId, id);
        r.setLibelle(req.libelle().trim());
        r.setObjectifId(req.objectifId());
        if (req.ordre() != null) {
            r.setOrdre(req.ordre());
        }
        return new ResultatResponse(r.getId(), r.getObjectifId(), r.getLibelle(), r.getOrdre());
    }

    /** Refusé tant que des activités ou des indicateurs mesurent ce résultat. */
    public void supprimerResultat(UUID projetId, UUID id, User user) {
        projetModifiable(projetId, user);
        ResultatAttendu r = resultat(projetId, id);
        boolean utilise = activiteRepository.findByProjetId(projetId).stream().anyMatch(a -> id.equals(a.getResultatId()))
                || indicateurs(projetId).stream().anyMatch(i -> id.equals(i.getResultatId()));
        if (utilise) {
            throw new BusinessException("Ce résultat a des activités ou des indicateurs : retirez-les d'abord");
        }
        resultatRepository.delete(r);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Budget prévisionnel
    // ═════════════════════════════════════════════════════════════════════

    /** Remplace tout le budget ; le budget total du projet devient la somme des lignes. */
    public List<LigneBudgetResponse> enregistrerBudget(UUID projetId, BudgetRequest req, User user) {
        Projet p = projetModifiable(projetId, user);
        Set<UUID> conventions = financementRepository.findByProjetId(projetId).stream()
                .map(Financement::getId).collect(Collectors.toSet());
        List<LigneBudgetProjet> lignes = new ArrayList<>();
        int ordre = 1;
        for (LigneBudgetRequest l : Optional.ofNullable(req.lignes()).orElse(List.of())) {
            if (!estRempli(l.intitule()) || l.categorie() == null) {
                throw new BusinessException("Chaque ligne du budget demande un intitulé et une rubrique");
            }
            if (l.montant() == null || l.montant().signum() <= 0) {
                throw new BusinessException("Le montant de « " + l.intitule().trim() + " » doit être positif");
            }
            if (l.financementId() != null && !conventions.contains(l.financementId())) {
                throw new BusinessException("La convention choisie n'appartient pas à ce projet");
            }
            LigneBudgetProjet ligne = new LigneBudgetProjet();
            ligne.setProjetId(projetId);
            ligne.setCode(estRempli(l.code()) ? l.code().trim() : null);
            ligne.setIntitule(l.intitule().trim());
            ligne.setCategorie(l.categorie());
            ligne.setMontant(l.montant());
            ligne.setFinancementId(l.financementId());
            ligne.setOrdre(ordre++);
            lignes.add(ligne);
        }
        budgetRepository.deleteByProjetId(projetId);
        budgetRepository.flush();
        List<LigneBudgetProjet> enregistrees = budgetRepository.saveAll(lignes);
        p.setBudgetTotal(enregistrees.stream().map(LigneBudgetProjet::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add));
        projetRepository.save(p);
        return enregistrees.stream()
                .map(l -> new LigneBudgetResponse(l.getId(), l.getCode(), l.getIntitule(), l.getCategorie(),
                        l.getMontant(), l.getFinancementId()))
                .toList();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Outils
    // ═════════════════════════════════════════════════════════════════════

    private Projet projetModifiable(UUID projetId, User user) {
        Projet p = projet(projetId);
        droits.verifierMontage(p, user);
        return p;
    }

    private Projet projet(UUID id) {
        return projetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + id));
    }

    private ResultatAttendu resultat(UUID projetId, UUID id) {
        return resultatRepository.findById(id).filter(r -> r.getProjetId().equals(projetId))
                .orElseThrow(() -> new ResourceNotFoundException("Résultat introuvable"));
    }

    private void verifierObjectif(UUID projetId, UUID objectifId) {
        if (objectifId != null && objectifRepository.findById(objectifId)
                .filter(o -> o.getProjetId().equals(projetId)).isEmpty()) {
            throw new BusinessException("Cet objectif n'appartient pas au projet");
        }
    }

    private List<Indicateur> indicateurs(UUID projetId) {
        return indicateurRepository.findByProjetId(projetId).stream()
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .toList();
    }

    private BigDecimal total(UUID projetId) {
        return budgetRepository.findByProjetIdOrderByOrdreAsc(projetId).stream()
                .map(LigneBudgetProjet::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean estRempli(String s) {
        return s != null && !s.isBlank();
    }
}
