package ong.fodde.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.RoleRepository;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.shared.exception.BusinessException;

/** Règles de suppression définitive d'un compte. */
class SuppressionUtilisateurTest {

    private final UserRepository users = mock(UserRepository.class);
    private final LiensUtilisateur liens = mock(LiensUtilisateur.class);
    private final UserService service = new UserService(users, mock(RoleRepository.class), mock(PasswordEncoder.class), liens);
    private final UUID admin = UUID.randomUUID();

    private User compte(String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setPrenom("Test");
        u.setNom(role);
        u.setStatut(StatutUser.ACTIF);
        Role r = new Role();
        r.setNom(role);
        u.setRoles(Set.of(r));
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void unCompteSansDonneesEstSupprime() {
        User u = compte("ROLE_CHARGE_PROGRAMME");
        when(liens.lister(u.getId())).thenReturn(List.of());
        service.supprimerUtilisateur(u.getId(), admin);
        verify(users).delete(u);
    }

    @Test
    void unCompteAvecDesDonneesEstRefuseAvecLeDetail() {
        User u = compte("ROLE_COMPTABLE");
        when(liens.lister(u.getId())).thenReturn(List.of("2 dépenses"));
        BusinessException e = assertThrows(BusinessException.class, () -> service.supprimerUtilisateur(u.getId(), admin));
        assertTrue(e.getMessage().contains("2 dépenses") && e.getMessage().contains("Désactivez"));
        verify(users, never()).delete(u);
    }

    @Test
    void onNePeutPasSupprimerSonPropreCompte() {
        User u = compte("ROLE_ADMIN");
        assertThrows(BusinessException.class, () -> service.supprimerUtilisateur(u.getId(), u.getId()));
        verify(users, never()).delete(u);
    }

    @Test
    void leDernierAdministrateurActifEstProtege() {
        User seul = compte("ROLE_ADMIN");
        when(users.findAll()).thenReturn(List.of(seul));
        assertThrows(BusinessException.class, () -> service.supprimerUtilisateur(seul.getId(), admin));
        verify(users, never()).delete(seul);
    }

    @Test
    void unAdministrateurPeutEtreSupprimeSIlEnResteUnAutre() {
        User a1 = compte("ROLE_ADMIN");
        User a2 = compte("ROLE_ADMIN");
        when(users.findAll()).thenReturn(List.of(a1, a2));
        when(liens.lister(a1.getId())).thenReturn(List.of());
        service.supprimerUtilisateur(a1.getId(), a2.getId());
        verify(users).delete(a1);
    }
}
