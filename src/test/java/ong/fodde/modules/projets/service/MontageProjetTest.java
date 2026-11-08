package ong.fodde.modules.projets.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.repository.FinancementRepository;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.projets.dto.ActiviteRequest;
import ong.fodde.modules.projets.dto.MontageDtos.BudgetRequest;
import ong.fodde.modules.projets.dto.MontageDtos.LigneBudgetRequest;
import ong.fodde.modules.projets.dto.MontageDtos.ResultatRequest;
import ong.fodde.modules.projets.dto.ProjetRequest;
import ong.fodde.modules.projets.dto.ProjetResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.CategorieBudget;
import ong.fodde.modules.projets.entity.LigneBudgetProjet;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.ResultatAttendu;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.BailleurRepository;
import ong.fodde.modules.projets.repository.LigneBudgetProjetRepository;
import ong.fodde.modules.projets.repository.ObjectifSpecifiqueRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.ResultatAttenduRepository;
import ong.fodde.modules.se.entity.Indicateur;
import ong.fodde.modules.se.repository.IndicateurRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Montage d'un projet : droits, cadre logique, budget, contrôles et circuit RP → RAF → SE. */
class MontageProjetTest {

    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ResultatAttenduRepository resultats = mock(ResultatAttenduRepository.class);
    private final ActiviteRepository activites = mock(ActiviteRepository.class);
    private final IndicateurRepository indicateurs = mock(IndicateurRepository.class);
    private final LigneBudgetProjetRepository budget = mock(LigneBudgetProjetRepository.class);
    private final FinancementRepository financements = mock(FinancementRepository.class);

    private final DroitsProjet droits = new DroitsProjet(new AccesPersonnel(mock(EmployeRepository.class)));
    private final ProjetMapper mapper = new ProjetMapper(users);
    private final ActiviteService activiteService = new ActiviteService(activites, projets, resultats, users, droits);
    private final MontageProjetService montage = new MontageProjetService(projets,
            mock(ObjectifSpecifiqueRepository.class), resultats, activites, indicateurs, budget, financements,
            mock(BailleurRepository.class), activiteService, droits, mapper);
    private final ProjetService service = new ProjetService(projets, users, montage, mock(HistoriqueService.class),
            droits, mapper);

    private final List<ResultatAttendu> lesResultats = new ArrayList<>();
    private final List<Activite> lesActivites = new ArrayList<>();
    private final List<Indicateur> lesIndicateurs = new ArrayList<>();
    private final List<LigneBudgetProjet> lesLignes = new ArrayList<>();

    private User rp;
    private User cp;

    @BeforeEach
    void preparer() {
        rp = compte("ROLE_RESPONSABLE_PROGRAMMES");
        cp = compte("ROLE_CHARGE_PROGRAMME");
        when(projets.save(any())).thenAnswer(i -> {
            Projet p = i.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            when(projets.findById(p.getId())).thenReturn(Optional.of(p));
            return p;
        });
        when(resultats.findByProjetIdOrderByOrdreAsc(any())).thenAnswer(i -> lesResultats);
        when(resultats.save(any())).thenAnswer(i -> {
            ResultatAttendu r = i.getArgument(0);
            r.setId(UUID.randomUUID());
            lesResultats.add(r);
            when(resultats.findById(r.getId())).thenReturn(Optional.of(r));
            return r;
        });
        when(activites.findByProjetId(any())).thenAnswer(i -> lesActivites);
        when(activites.save(any())).thenAnswer(i -> {
            Activite a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            lesActivites.add(a);
            return a;
        });
        when(indicateurs.findByProjetId(any())).thenAnswer(i -> lesIndicateurs);
        when(budget.findByProjetIdOrderByOrdreAsc(any())).thenAnswer(i -> lesLignes);
        when(budget.saveAll(anyList())).thenAnswer(i -> {
            List<LigneBudgetProjet> l = i.getArgument(0);
            lesLignes.clear();
            lesLignes.addAll(l);
            return l;
        });
        when(financements.findByProjetId(any())).thenReturn(List.of());
    }

    private User compte(String... roles) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom("Ba");
        u.setPrenom(roles.length > 0 ? roles[0] : "Sans rôle");
        Set<Role> set = new HashSet<>();
        for (String nom : roles) {
            Role r = new Role();
            r.setNom(nom);
            set.add(r);
        }
        u.setRoles(set);
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        when(users.existsById(u.getId())).thenReturn(true);
        return u;
    }

    private ProjetResponse nouveauProjet() {
        return service.creerProjet(new ProjetRequest(null, "Maraîchage à Kolda", null, "Améliorer la sécurité alimentaire",
                "Kolda", LocalDate.of(2027, 1, 1), LocalDate.of(2028, 12, 31), null, null, cp.getId(), null), rp);
    }

    /** Résultat, activité, indicateur et budget : un projet prêt à soumettre. */
    private ProjetResponse projetComplet() {
        ProjetResponse p = nouveauProjet();
        UUID r = montage.ajouterResultat(p.id(), new ResultatRequest("Production accrue", null, null), cp).id();
        activiteService.creerActivite(new ActiviteRequest(p.id(), null, r, "Former 100 maraîchers", null, cp.getId(),
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 3, 1), null), cp);
        Indicateur ind = new Indicateur();
        ind.setResultatId(r);
        lesIndicateurs.add(ind);
        montage.enregistrerBudget(p.id(), new BudgetRequest(List.of(
                new LigneBudgetRequest("1.1", "Formations", CategorieBudget.ACTIVITES, new BigDecimal("3000000"), null),
                new LigneBudgetRequest("2.1", "Animateur", CategorieBudget.PERSONNEL, new BigDecimal("2000000"), null))), rp);
        return p;
    }

    // ── Création ──────────────────────────────────────────

    @Test
    void leCodeEstAttribueEtLeChefRejointLEquipe() {
        ProjetResponse p = nouveauProjet();
        assertTrue(p.code().matches("PROJ-\\d{4}-001"));
        assertEquals(StatutProjet.BROUILLON, p.statut());
        assertEquals(rp.getId(), p.responsableId());
        assertTrue(projets.findById(p.id()).orElseThrow().getEquipe().contains(cp));
    }

    // ── Droits de montage ─────────────────────────────────

    @Test
    void unAutreChargeDeProgrammeNeMontePasLeProjet() {
        ProjetResponse p = nouveauProjet();
        User autre = compte("ROLE_CHARGE_PROGRAMME");
        assertThrows(AccessDeniedException.class,
                () -> montage.ajouterResultat(p.id(), new ResultatRequest("R", null, null), autre));
    }

    @Test
    void unProjetSoumisNeSeModifiePlus() {
        ProjetResponse p = projetComplet();
        service.soumettre(p.id(), rp);
        assertThrows(BusinessException.class,
                () -> montage.ajouterResultat(p.id(), new ResultatRequest("R2", null, null), rp));
    }

    @Test
    void uneActiviteHorsDesDatesDuProjetEstRefusee() {
        ProjetResponse p = nouveauProjet();
        assertThrows(BusinessException.class, () -> activiteService.creerActivite(new ActiviteRequest(p.id(), null,
                null, "Trop tard", null, null, LocalDate.of(2029, 1, 1), LocalDate.of(2029, 2, 1), null), rp));
    }

    @Test
    void unResultatMesureNeSeSupprimePas() {
        ProjetResponse p = projetComplet();
        UUID r = lesResultats.get(0).getId();
        assertThrows(BusinessException.class, () -> montage.supprimerResultat(p.id(), r, rp));
    }

    // ── Budget ────────────────────────────────────────────

    @Test
    void leBudgetTotalEstLaSommeDesLignes() {
        ProjetResponse p = projetComplet();
        assertEquals(0, new BigDecimal("5000000").compareTo(projets.findById(p.id()).orElseThrow().getBudgetTotal()));
    }

    @Test
    void uneLigneSansMontantEstRefusee() {
        ProjetResponse p = nouveauProjet();
        assertThrows(BusinessException.class, () -> montage.enregistrerBudget(p.id(), new BudgetRequest(List.of(
                new LigneBudgetRequest(null, "Véhicule", CategorieBudget.EQUIPEMENT, BigDecimal.ZERO, null))), rp));
    }

    // ── Contrôles et circuit ──────────────────────────────

    @Test
    void unProjetIncompletNeSeSoumetPas() {
        ProjetResponse p = nouveauProjet();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.soumettre(p.id(), rp));
        assertTrue(ex.getMessage().contains("résultat attendu"));
        assertTrue(ex.getMessage().contains("Budget"));
    }

    @Test
    void lesConventionsManquantesNeBloquentPas() {
        ProjetResponse p = projetComplet();
        var controles = montage.controles(projets.findById(p.id()).orElseThrow());
        assertTrue(controles.stream().filter(c -> c.bloquant()).allMatch(c -> c.ok()));
        assertFalse(controles.stream().filter(c -> !c.bloquant()).findFirst().orElseThrow().ok());
    }

    @Test
    void leCircuitVaDuRpAuRafPuisAuSe() {
        ProjetResponse p = projetComplet();
        assertEquals(StatutProjet.EN_VALIDATION, service.soumettre(p.id(), cp).statut());
        assertEquals(StatutProjet.EN_VALIDATION_SE, service.verifierBudget(p.id(), compte("ROLE_RAF")).statut());
        assertEquals(StatutProjet.ACTIF, service.valider(p.id(), compte("ROLE_SE")).statut());
    }

    @Test
    void leRafRenvoieAvecUnMotifEtPasAlEtapeDuSe() {
        ProjetResponse p = projetComplet();
        service.soumettre(p.id(), rp);
        User raf = compte("ROLE_RAF");
        assertThrows(BusinessException.class, () -> service.renvoyer(p.id(), " ", raf));
        ProjetResponse renvoye = service.renvoyer(p.id(), "Revoir le personnel", raf);
        assertEquals(StatutProjet.BROUILLON, renvoye.statut());
        assertEquals("Revoir le personnel", renvoye.motifRenvoi());

        service.soumettre(p.id(), rp);
        service.verifierBudget(p.id(), raf);
        assertThrows(AccessDeniedException.class, () -> service.renvoyer(p.id(), "Non", raf));
    }

    @Test
    void laRouteGeneriqueNeSautePasLaValidation() {
        ProjetResponse p = projetComplet();
        assertThrows(BusinessException.class,
                () -> service.changerStatut(p.id(), StatutProjet.ACTIF, compte("ROLE_SE"), null));
    }
}
