package ong.fodde.modules.historique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.financier.repository.DepenseRepository;
import ong.fodde.modules.financier.repository.LigneBudgetaireRepository;
import ong.fodde.modules.financier.repository.PieceJustificativeRepository;
import ong.fodde.modules.financier.service.DepenseService;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.historique.entity.HistoriqueDemande;
import ong.fodde.modules.historique.entity.TypeDemande;
import ong.fodde.modules.historique.repository.HistoriqueDemandeRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Historique des validations : enregistrement, assemblage avec les anciennes demandes, accès. */
class HistoriqueServiceTest {

    private static final EtapeHistorique CREATION =
            new EtapeHistorique(null, "BROUILLON", null, "Awa Diallo", null, LocalDateTime.now(), true);

    private static HistoriqueDemande ligne(String avant, String apres, String auteur) {
        HistoriqueDemande h = new HistoriqueDemande();
        h.setStatutAvant(avant);
        h.setStatutApres(apres);
        h.setAuteurNom(auteur);
        h.setDateAction(LocalDateTime.now());
        return h;
    }

    // ── Assemblage ─────────────────────────────────────────────────────────

    @Test
    void demandeAncienneSansJournalOnReconstitue() {
        EtapeHistorique validation = new EtapeHistorique(null, "VALIDEE", null, "RAF", null, null, true);
        List<EtapeHistorique> e = HistoriqueService.assembler(List.of(), CREATION, List.of(validation));
        assertEquals(List.of("BROUILLON", "VALIDEE"), e.stream().map(EtapeHistorique::statutApres).toList());
        assertTrue(e.get(1).reconstituee());
    }

    @Test
    void demandeNouvelleOnLitLeJournalSeul() {
        List<EtapeHistorique> e = HistoriqueService.assembler(
                List.of(ligne(null, "BROUILLON", "Comptable"), ligne("BROUILLON", "SOUMISE", "Comptable"),
                        ligne("SOUMISE", "VERIFIEE", "RAF")),
                CREATION,
                List.of(new EtapeHistorique(null, "VERIFIEE", null, "doublon", null, null, true)));
        assertEquals(List.of("BROUILLON", "SOUMISE", "VERIFIEE"), e.stream().map(EtapeHistorique::statutApres).toList());
        assertTrue(e.stream().noneMatch(EtapeHistorique::reconstituee));
    }

    @Test
    void demandeAncienneTraiteeApresLaMiseEnPlaceOnAjouteLaCreation() {
        // Créée avant l'historique, soumise après : la création est reconstituée, la suite vient du journal.
        List<EtapeHistorique> e = HistoriqueService.assembler(
                List.of(ligne("BROUILLON", "SOUMISE", "Comptable")), CREATION, List.of());
        assertEquals(2, e.size());
        assertTrue(e.get(0).reconstituee());
        assertFalse(e.get(1).reconstituee());
    }

    // ── Enregistrement par le circuit des dépenses ─────────────────────────

    private final DepenseRepository depenses = mock(DepenseRepository.class);
    private final PieceJustificativeRepository pieces = mock(PieceJustificativeRepository.class);
    private final HistoriqueService historique = mock(HistoriqueService.class);
    private final EmployeRepository employes = mock(EmployeRepository.class);
    private final DepenseService depenseService = new DepenseService(depenses, mock(LigneBudgetaireRepository.class),
            mock(ProjetRepository.class), mock(UserRepository.class), pieces, historique,
            new AccesPersonnel(employes), new BigDecimal("100000"));

    private Depense depense(StatutDepense statut, UUID auteur) {
        Depense d = new Depense();
        d.setId(UUID.randomUUID());
        d.setMontant(new BigDecimal("50000"));
        d.setStatut(statut);
        d.setLigneBudgetaireId(UUID.randomUUID());
        d.setProjetId(UUID.randomUUID());
        d.setSaisiParId(auteur);
        when(depenses.findById(d.getId())).thenReturn(Optional.of(d));
        when(depenses.save(any())).thenAnswer(i -> i.getArgument(0));
        return d;
    }

    @Test
    void unRejetEstJournaliseAvecSonAuteurEtSonMotif() {
        Depense d = depense(StatutDepense.SOUMISE, UUID.randomUUID());
        UUID raf = UUID.randomUUID();
        depenseService.changerStatut(d.getId(), StatutDepense.REJETEE, raf, "Facture illisible");
        verify(historique).enregistrer(TypeDemande.DEPENSE, d.getId(), StatutDepense.SOUMISE,
                StatutDepense.REJETEE, raf, "Facture illisible");
    }

    @Test
    void uneTransitionRefuseeNEstPasJournalisee() {
        Depense d = depense(StatutDepense.BROUILLON, UUID.randomUUID());
        assertThrows(BusinessException.class,
                () -> depenseService.changerStatut(d.getId(), StatutDepense.VALIDEE, UUID.randomUUID(), null));
        verify(historique, never()).enregistrer(any(), any(), any(), any(), any(), any());
    }

    @Test
    void uneAnnulationEstJournaliseeDepuisLeStatutQuitte() {
        Depense d = depense(StatutDepense.VERIFIEE, UUID.randomUUID());
        UUID auteur = UUID.randomUUID();
        depenseService.annulerDepense(d.getId(), "Doublon", auteur);
        verify(historique).enregistrer(eq(TypeDemande.DEPENSE), eq(d.getId()), eq(StatutDepense.VERIFIEE),
                eq(StatutDepense.ANNULEE), eq(auteur), eq("Doublon"));
    }

    // ── Accès à l'historique d'une dépense ─────────────────────────────────

    private static User utilisateur(String... roles) {
        User u = new User();
        u.setId(UUID.randomUUID());
        for (String r : roles) {
            ong.fodde.modules.auth.entity.Role role = new ong.fodde.modules.auth.entity.Role();
            role.setNom(r);
            u.getRoles().add(role);
        }
        return u;
    }

    @Test
    void unAutreComptableNePeutPasLireLHistorique() {
        Depense d = depense(StatutDepense.SOUMISE, UUID.randomUUID());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> depenseService.historique(d.getId(), utilisateur("ROLE_COMPTABLE")));
    }

    @Test
    void lAuteurEtLeRafPeuventLireLHistorique() {
        User comptable = utilisateur("ROLE_COMPTABLE");
        Depense d = depense(StatutDepense.SOUMISE, comptable.getId());
        depenseService.historique(d.getId(), comptable);
        depenseService.historique(d.getId(), utilisateur("ROLE_RAF"));
        verify(historique, org.mockito.Mockito.times(2))
                .historique(eq(TypeDemande.DEPENSE), eq(d.getId()), any(), any());
    }

    // ── Enregistrement réel (dépôt simulé) ─────────────────────────────────

    @Test
    void enregistrerFigeLeNomDeLAuteurEtIgnoreUnMotifVide() {
        HistoriqueDemandeRepository repo = mock(HistoriqueDemandeRepository.class);
        UserRepository users = mock(UserRepository.class);
        User raf = utilisateur("ROLE_RAF");
        raf.setPrenom("Moussa");
        raf.setNom("Ba");
        when(users.findById(raf.getId())).thenReturn(Optional.of(raf));
        new HistoriqueService(repo, users).enregistrer(TypeDemande.MISSION, UUID.randomUUID(),
                null, StatutDepense.SOUMISE, raf.getId(), "  ");
        ArgumentCaptor<HistoriqueDemande> c = ArgumentCaptor.forClass(HistoriqueDemande.class);
        verify(repo).save(c.capture());
        assertEquals("Moussa Ba", c.getValue().getAuteurNom());
        assertNull(c.getValue().getMotif());
        assertNull(c.getValue().getStatutAvant());
    }
}
