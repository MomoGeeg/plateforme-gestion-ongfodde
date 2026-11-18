package ong.fodde.modules.rh.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.rh.dto.DemandeCongeRequest;
import ong.fodde.modules.rh.dto.DemandeCongeResponse;
import ong.fodde.modules.rh.entity.DemandeConge;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.entity.StatutConge;
import ong.fodde.modules.rh.entity.TypeConge;
import ong.fodde.modules.rh.repository.DemandeCongeRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;

/** Circuit des congés : l'employé demande, les RH valident, le RAF approuve. */
class CircuitCongeTest {

    private final DemandeCongeRepository conges = mock(DemandeCongeRepository.class);
    private final EmployeRepository employes = mock(EmployeRepository.class);
    private final DemandeCongeService service = new DemandeCongeService(conges, employes, mock(HistoriqueService.class));

    private final User agent = compte("ROLE_TECHNICIEN_TERRAIN");
    private final User rh = compte("ROLE_RH");
    private final User raf = compte("ROLE_RAF");
    private final Employe ficheAgent = fiche(agent, 20);

    CircuitCongeTest() {
        when(conges.save(any(DemandeConge.class))).thenAnswer(i -> {
            DemandeConge d = i.getArgument(0);
            if (d.getId() == null) d.setId(UUID.randomUUID());
            when(conges.findById(d.getId())).thenReturn(Optional.of(d));
            return d;
        });
        when(employes.findByUserId(rh.getId())).thenReturn(Optional.empty());
        when(employes.findByUserId(raf.getId())).thenReturn(Optional.empty());
    }

    private static User compte(String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        Role r = new Role();
        r.setNom(role);
        u.setRoles(Set.of(r));
        return u;
    }

    private Employe fiche(User u, int solde) {
        Employe e = new Employe();
        e.setId(UUID.randomUUID());
        e.setUserId(u.getId());
        e.setSoldeConges(solde);
        when(employes.findByUserId(u.getId())).thenReturn(Optional.of(e));
        when(employes.findById(e.getId())).thenReturn(Optional.of(e));
        return e;
    }

    private DemandeCongeRequest demande(UUID employeId, int jours) {
        LocalDate debut = LocalDate.of(2026, 12, 1);
        return new DemandeCongeRequest(employeId, TypeConge.ANNUEL, debut, debut.plusDays(jours - 1), "Repos", null);
    }

    @Test
    void lEmployeDemandePourLuiEtLaDemandePartAuxRH() {
        DemandeCongeResponse r = service.soumettreDemande(demande(null, 3), agent);
        assertEquals(StatutConge.EN_ATTENTE, r.statut());
        assertEquals(ficheAgent.getId(), r.employeId());
        assertEquals(3, r.nbJours(), "du 1er au 3 décembre = 3 jours");
    }

    @Test
    void circuitCompletRhPuisRafDebiteLeSolde() {
        UUID id = service.soumettreDemande(demande(null, 5), agent).id();
        assertEquals(StatutConge.VALIDE_RH, service.validerRH(id, rh.getId()).statut());
        assertEquals(StatutConge.APPROUVE, service.approuverRAF(id, raf.getId()).statut());
        assertEquals(15, ficheAgent.getSoldeConges());
    }

    @Test
    void leRafNePeutPasApprouverAvantLesRH() {
        UUID id = service.soumettreDemande(demande(null, 2), agent).id();
        assertThrows(BusinessException.class, () -> service.approuverRAF(id, raf.getId()));
    }

    @Test
    void lesRHEnregistrentPourUnEmployeEtCelaPartDirectementAuRAF() {
        DemandeCongeResponse r = service.soumettreDemande(demande(ficheAgent.getId(), 2), rh);
        assertEquals(StatutConge.VALIDE_RH, r.statut());
        assertEquals(rh.getId(), r.valideParRh());
    }

    @Test
    void unEmployeNePeutPasDemanderPourUnAutre() {
        User autre = compte("ROLE_COMPTABLE");
        fiche(autre, 10);
        assertThrows(AccessDeniedException.class, () -> service.soumettreDemande(demande(ficheAgent.getId(), 2), autre));
    }

    @Test
    void sansFicheLiéeLaDemandeEstRefusee() {
        User sansFiche = compte("ROLE_COMPTABLE");
        when(employes.findByUserId(sansFiche.getId())).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.soumettreDemande(demande(null, 2), sansFiche));
    }

    @Test
    void chacunRejetteASonEtapeAvecUnMotif() {
        UUID id = service.soumettreDemande(demande(null, 2), agent).id();
        assertThrows(AccessDeniedException.class, () -> service.rejeter(id, raf, "Période chargée"));
        assertThrows(BusinessException.class, () -> service.rejeter(id, rh, " "));
        assertEquals(StatutConge.REJETE, service.rejeter(id, rh, "Période chargée").statut());

        UUID id2 = service.soumettreDemande(demande(null, 2), agent).id();
        service.validerRH(id2, rh.getId());
        assertThrows(AccessDeniedException.class, () -> service.rejeter(id2, rh, "Non"));
        assertEquals(StatutConge.REJETE, service.rejeter(id2, raf, "Budget mission").statut());
    }

    @Test
    void seulLEmployeOuLesRHAnnulent() {
        UUID id = service.soumettreDemande(demande(null, 2), agent).id();
        assertThrows(AccessDeniedException.class, () -> service.annuler(id, raf));
        assertEquals(StatutConge.ANNULE, service.annuler(id, agent).statut());
    }

    @Test
    void soldeInsuffisant() {
        assertThrows(BusinessException.class, () -> service.soumettreDemande(demande(null, 25), agent));
    }
}
