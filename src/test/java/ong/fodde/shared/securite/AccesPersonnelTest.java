package ong.fodde.shared.securite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.repository.EmployeRepository;

class AccesPersonnelTest {

    private final EmployeRepository employes = mock(EmployeRepository.class);
    private final AccesPersonnel acces = new AccesPersonnel(employes);

    private static User utilisateur(String... roles) {
        User u = new User();
        u.setId(UUID.randomUUID());
        Set<Role> set = new java.util.HashSet<>();
        for (String r : roles) {
            Role role = new Role();
            role.setNom(r);
            set.add(role);
        }
        u.setRoles(set);
        return u;
    }

    @Test
    void lePropreUtilisateurAccedeASesDonnees() {
        User u = utilisateur("ROLE_CHARGE_PROGRAMME");
        assertDoesNotThrow(() -> acces.verifierUtilisateur(u.getId(), u, "ROLE_RAF"));
    }

    @Test
    void unAutreUtilisateurSansRoleEstRefuse() {
        User u = utilisateur("ROLE_CHARGE_PROGRAMME");
        assertThrows(AccessDeniedException.class, () -> acces.verifierUtilisateur(UUID.randomUUID(), u, "ROLE_RAF"));
    }

    @Test
    void unRoleDeGestionAccedeAuxDonneesDesAutres() {
        User raf = utilisateur("ROLE_RAF");
        assertDoesNotThrow(() -> acces.verifierUtilisateur(UUID.randomUUID(), raf, "ROLE_RAF"));
    }

    @Test
    void lEmployeAccedeASaPropreFiche() {
        User u = utilisateur("ROLE_TECHNICIEN_TERRAIN");
        Employe e = new Employe();
        e.setId(UUID.randomUUID());
        when(employes.findByUserId(u.getId())).thenReturn(Optional.of(e));
        assertDoesNotThrow(() -> acces.verifierEmploye(e.getId(), u, "ROLE_RH"));
        assertThrows(AccessDeniedException.class, () -> acces.verifierEmploye(UUID.randomUUID(), u, "ROLE_RH"));
    }

    @Test
    void unCompteSansFicheEmployeEstRefuse() {
        User u = utilisateur("ROLE_COMPTABLE");
        when(employes.findByUserId(u.getId())).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> acces.verifierEmploye(UUID.randomUUID(), u, "ROLE_RH"));
    }
}
