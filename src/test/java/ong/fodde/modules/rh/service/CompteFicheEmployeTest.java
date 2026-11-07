package ong.fodde.modules.rh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.rh.entity.DemandeConge;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.repository.ContratRepository;
import ong.fodde.modules.rh.repository.DemandeCongeRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/** Lien entre un compte utilisateur et une fiche employé, et « mes congés ». */
class CompteFicheEmployeTest {

    private final EmployeRepository employes = mock(EmployeRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EmployeService service = new EmployeService(employes, mock(ContratRepository.class), users);

    private Employe fiche(UUID userId) {
        Employe e = new Employe();
        e.setId(UUID.randomUUID());
        e.setNom("Diallo");
        e.setPrenom("Awa");
        e.setUserId(userId);
        when(employes.findById(e.getId())).thenReturn(Optional.of(e));
        when(employes.save(any())).thenAnswer(i -> i.getArgument(0));
        return e;
    }

    @Test
    void unCompteLibreEstAssocie() {
        Employe e = fiche(null);
        UUID compte = UUID.randomUUID();
        when(users.existsById(compte)).thenReturn(true);
        when(employes.findByUserId(compte)).thenReturn(Optional.empty());
        assertEquals(compte, service.associerCompte(e.getId(), compte).userId());
    }

    @Test
    void unCompteDejaLieAUneAutreFicheEstRefuse() {
        Employe e = fiche(null);
        UUID compte = UUID.randomUUID();
        Employe autre = new Employe();
        autre.setId(UUID.randomUUID());
        autre.setPrenom("Moussa");
        autre.setNom("Ba");
        when(users.existsById(compte)).thenReturn(true);
        when(employes.findByUserId(compte)).thenReturn(Optional.of(autre));
        BusinessException ex = assertThrows(BusinessException.class, () -> service.associerCompte(e.getId(), compte));
        assertTrue(ex.getMessage().contains("Moussa Ba"));
        verify(employes, never()).save(any());
    }

    @Test
    void uneFicheDejaLieeAUnAutreCompteEstRefusee() {
        Employe e = fiche(UUID.randomUUID());
        assertThrows(BusinessException.class, () -> service.associerCompte(e.getId(), UUID.randomUUID()));
        verify(employes, never()).save(any());
    }

    @Test
    void reassocierLeMemeCompteNeChangeRien() {
        UUID compte = UUID.randomUUID();
        Employe e = fiche(compte);
        assertEquals(compte, service.associerCompte(e.getId(), compte).userId());
        verify(employes, never()).save(any());
    }

    @Test
    void unCompteInexistantEstRefuse() {
        Employe e = fiche(null);
        UUID compte = UUID.randomUUID();
        when(users.existsById(compte)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.associerCompte(e.getId(), compte));
    }

    @Test
    void sansCompteChoisiOnRefuse() {
        Employe e = fiche(null);
        assertThrows(BusinessException.class, () -> service.associerCompte(e.getId(), null));
    }

    @Test
    void dissocierRetireLeLien() {
        Employe e = fiche(UUID.randomUUID());
        assertNull(service.dissocierCompte(e.getId()).userId());
    }

    @Test
    void maFicheEstVideSansLien() {
        when(employes.findByUserId(any())).thenReturn(Optional.empty());
        assertTrue(service.maFiche(UUID.randomUUID()).isEmpty());
    }

    // ── Comptes sans fiche (à compléter par les RH) ────────────────────────

    private static ong.fodde.modules.auth.entity.User compte(String prenom, String nom,
            ong.fodde.modules.auth.entity.StatutUser statut) {
        ong.fodde.modules.auth.entity.User u = new ong.fodde.modules.auth.entity.User();
        u.setId(UUID.randomUUID());
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setEmail(prenom.toLowerCase() + "@fodde.org");
        u.setStatut(statut);
        return u;
    }

    @Test
    void lesComptesSansFicheExcluentLesComptesLiesEtDesactives() {
        var lie = compte("Awa", "Diallo", ong.fodde.modules.auth.entity.StatutUser.ACTIF);
        var libre = compte("Ibrahima", "Sow", ong.fodde.modules.auth.entity.StatutUser.ACTIF);
        var enAttente = compte("Moussa", "Ba", ong.fodde.modules.auth.entity.StatutUser.EN_ATTENTE);
        var desactive = compte("Fatou", "Ndiaye", ong.fodde.modules.auth.entity.StatutUser.INACTIF);
        Employe e = new Employe();
        e.setUserId(lie.getId());
        when(employes.findAll()).thenReturn(List.of(e));
        when(users.findAll()).thenReturn(List.of(lie, libre, enAttente, desactive));

        var resultat = service.listerComptesSansFiche();
        assertEquals(List.of("Ba", "Sow"), resultat.stream().map(c -> c.nom()).toList());
    }

    // ── Mes congés ─────────────────────────────────────────────────────────

    private final DemandeCongeRepository conges = mock(DemandeCongeRepository.class);
    private final DemandeCongeService congeService =
            new DemandeCongeService(conges, employes, mock(HistoriqueService.class));

    @Test
    void mesCongesPasseParLaFicheLieeAuCompte() {
        UUID compte = UUID.randomUUID();
        Employe e = fiche(compte);
        when(employes.findByUserId(compte)).thenReturn(Optional.of(e));
        DemandeConge c = new DemandeConge();
        c.setEmployeId(e.getId());
        when(conges.findByEmployeId(e.getId())).thenReturn(List.of(c));
        assertEquals(1, congeService.mesCongesUtilisateur(compte).size());
    }

    @Test
    void sansFicheLieeLaListeEstVide() {
        when(employes.findByUserId(any())).thenReturn(Optional.empty());
        assertTrue(congeService.mesCongesUtilisateur(UUID.randomUUID()).isEmpty());
        verify(conges, never()).findByEmployeId(any());
    }
}
