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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.rh.dto.EmployeRequest;
import ong.fodde.modules.rh.dto.OrganisationDtos.PropositionRattachement;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.entity.Pole;
import ong.fodde.modules.rh.entity.StatutEmploye;
import ong.fodde.modules.rh.repository.ContratRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;

/** Pôle et supérieur direct : contrôles de la hiérarchie et proposition d'après les rôles. */
class OrganisationTest {

    private final EmployeRepository employes = mock(EmployeRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EmployeService service = new EmployeService(employes, mock(ContratRepository.class), users);
    private final OrganisationService organisation = new OrganisationService(employes, users);

    private Employe fiche(String prenom, UUID superieurId, UUID userId) {
        Employe e = new Employe();
        e.setId(UUID.randomUUID());
        e.setNom("Ba");
        e.setPrenom(prenom);
        e.setPoste("Poste");
        e.setStatut(StatutEmploye.ACTIF);
        e.setSuperieurId(superieurId);
        e.setUserId(userId);
        when(employes.findById(e.getId())).thenReturn(Optional.of(e));
        if (userId != null) {
            when(employes.findByUserId(userId)).thenReturn(Optional.of(e));
        }
        when(employes.save(any())).thenAnswer(i -> i.getArgument(0));
        return e;
    }

    private User compte(String... roles) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom("Compte");
        u.setPrenom("Test");
        u.setStatut(StatutUser.ACTIF);
        Set<Role> set = new java.util.HashSet<>();
        for (String nom : roles) {
            Role r = new Role();
            r.setNom(nom);
            set.add(r);
        }
        u.setRoles(set);
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    private EmployeRequest demande(Pole pole, UUID superieurId) {
        return new EmployeRequest(null, "EMP-1", "Ba", "Awa", null, null, "Sénégalaise", null, null,
                "Chargée de programme", "Programmes", LocalDate.of(2026, 1, 1), pole, superieurId);
    }

    // ── Contrôles du supérieur ─────────────────────────────

    @Test
    void lePoleEtLeSuperieurSontEnregistres() {
        Employe rp = fiche("Rp", null, null);
        Employe cp = fiche("Cp", null, null);
        var r = service.modifierEmploye(cp.getId(), demande(Pole.PROGRAMMES, rp.getId()));
        assertEquals(Pole.PROGRAMMES, r.pole());
        assertEquals(rp.getId(), r.superieurId());
        assertEquals("Rp Ba", r.superieurNom());
    }

    @Test
    void personneNEstSonPropreSuperieur() {
        Employe e = fiche("Awa", null, null);
        assertThrows(BusinessException.class, () -> service.modifierEmploye(e.getId(), demande(Pole.RAF, e.getId())));
        verify(employes, never()).save(any());
    }

    @Test
    void unSuperieurPartiEstRefuse() {
        Employe parti = fiche("Parti", null, null);
        parti.setStatut(StatutEmploye.PARTI);
        Employe e = fiche("Awa", null, null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.modifierEmploye(e.getId(), demande(Pole.RAF, parti.getId())));
        assertTrue(ex.getMessage().contains("a quitté"));
    }

    @Test
    void laHierarchieNeTournePasEnRond() {
        // A est le chef de B, B le chef de C : C ne peut pas devenir le chef de A.
        Employe a = fiche("A", null, null);
        Employe b = fiche("B", a.getId(), null);
        Employe c = fiche("C", b.getId(), null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.modifierEmploye(a.getId(), demande(Pole.DIRECTION, c.getId())));
        assertTrue(ex.getMessage().contains("dépend déjà"));
        verify(employes, never()).save(any());
    }

    @Test
    void sansSuperieurCestAccepte() {
        Employe e = fiche("Se", null, null);
        assertNull(service.modifierEmploye(e.getId(), demande(Pole.DIRECTION, null)).superieurId());
    }

    // ── Proposition d'après les rôles ──────────────────────

    @Test
    void unChargeDeProgrammeDependDuRp() {
        User rp = compte("ROLE_RESPONSABLE_PROGRAMMES");
        Employe ficheRp = fiche("Rp", null, rp.getId());
        User cp = compte("ROLE_CHARGE_PROGRAMME");
        when(users.findAll()).thenReturn(List.of(rp, cp));
        PropositionRattachement p = organisation.proposer(cp.getId());
        assertEquals(Pole.PROGRAMMES, p.pole());
        assertEquals(ficheRp.getId(), p.superieurId());
    }

    @Test
    void lesRhSontAuPoleRafSousLeRaf() {
        User raf = compte("ROLE_RAF");
        Employe ficheRaf = fiche("Raf", null, raf.getId());
        User rh = compte("ROLE_RH");
        when(users.findAll()).thenReturn(List.of(raf, rh));
        PropositionRattachement p = organisation.proposer(rh.getId());
        assertEquals(Pole.RAF, p.pole());
        assertEquals(ficheRaf.getId(), p.superieurId());
    }

    @Test
    void leRafDependDuSe() {
        User se = compte("ROLE_SE");
        Employe ficheSe = fiche("Se", null, se.getId());
        User raf = compte("ROLE_RAF", "ROLE_COMPTABLE"); // le plus haut rôle l'emporte
        when(users.findAll()).thenReturn(List.of(se, raf));
        PropositionRattachement p = organisation.proposer(raf.getId());
        assertEquals(Pole.RAF, p.pole());
        assertEquals(ficheSe.getId(), p.superieurId());
    }

    @Test
    void plusieursChefsPossiblesRienNEstImpose() {
        User raf1 = compte("ROLE_RAF");
        fiche("Raf1", null, raf1.getId());
        User raf2 = compte("ROLE_RAF");
        fiche("Raf2", null, raf2.getId());
        User comptable = compte("ROLE_COMPTABLE");
        when(users.findAll()).thenReturn(List.of(raf1, raf2, comptable));
        PropositionRattachement p = organisation.proposer(comptable.getId());
        assertEquals(Pole.RAF, p.pole());
        assertNull(p.superieurId());
        assertTrue(p.message().contains("2 personnes"));
    }

    @Test
    void unChefSansFicheNEstPasPropose() {
        User rp = compte("ROLE_RESPONSABLE_PROGRAMMES"); // pas de fiche
        User tech = compte("ROLE_TECHNICIEN_TERRAIN");
        when(users.findAll()).thenReturn(List.of(rp, tech));
        PropositionRattachement p = organisation.proposer(tech.getId());
        assertNull(p.superieurId());
        assertTrue(p.message().contains("Aucune fiche"));
    }

    @Test
    void leSeEstEnHautSansSuperieur() {
        User se = compte("ROLE_SE");
        when(users.findAll()).thenReturn(List.of(se));
        PropositionRattachement p = organisation.proposer(se.getId());
        assertEquals(Pole.DIRECTION, p.pole());
        assertNull(p.superieurId());
    }
}
