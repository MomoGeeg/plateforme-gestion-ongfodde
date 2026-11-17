package ong.fodde.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.config.JwtService;
import ong.fodde.modules.auth.dto.LoginRequest;
import ong.fodde.modules.auth.dto.UserListResponse;
import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.RoleRepository;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.shared.exception.AuthentificationException;
import ong.fodde.shared.exception.CompteVerrouilleException;

/** Verrouillage après des mots de passe incorrects successifs, et déblocage par l'admin. */
class VerrouillageCompteTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final AuthService auth = new AuthService(users, encoder, mock(JwtService.class), mock(JournalConnexions.class));
    private final UserService userService = new UserService(users, mock(RoleRepository.class), encoder, mock(LiensUtilisateur.class));

    private User compte(int tentatives) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("raf@fodde.org");
        u.setPrenom("Awa");
        u.setNom("Diop");
        u.setStatut(StatutUser.ACTIF);
        u.setTentativesEchouees(tentatives);
        u.setMotDePasseHash("hash");
        Role r = new Role();
        r.setNom("ROLE_RAF");
        u.setRoles(Set.of(r));
        when(users.findByEmail("raf@fodde.org")).thenReturn(Optional.of(u));
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        return u;
    }

    @Test
    void unRefusNAnnulePasLaTransaction() throws Exception {
        Transactional t = AuthService.class
                .getMethod("login", LoginRequest.class, String.class, String.class)
                .getAnnotation(Transactional.class);
        List<Class<? extends Throwable>> sansAnnulation = List.of(t.noRollbackFor());
        assertTrue(sansAnnulation.contains(AuthentificationException.class));
        assertTrue(sansAnnulation.contains(CompteVerrouilleException.class));
    }

    @Test
    void unMauvaisMotDePasseIncrementeLeCompteur() {
        User u = compte(2);
        assertThrows(AuthentificationException.class,
                () -> auth.login(new LoginRequest("raf@fodde.org", "faux"), "ip", "nav"));
        assertEquals(3, u.getTentativesEchouees());
        verify(users).save(u);
    }

    @Test
    void auCinquiemeEchecLeCompteEstVerrouilleMemeAvecLeBonMotDePasse() {
        compte(AuthService.TENTATIVES_MAX);
        when(encoder.matches("bon", "hash")).thenReturn(true);
        assertThrows(CompteVerrouilleException.class,
                () -> auth.login(new LoginRequest("raf@fodde.org", "bon"), "ip", "nav"));
    }

    @Test
    void laListeIndiqueLeVerrouillage() {
        User u = compte(AuthService.TENTATIVES_MAX);
        assertTrue(userService.obtenirUtilisateur(u.getId()).verrouille());
        u.setTentativesEchouees(4);
        assertFalse(userService.obtenirUtilisateur(u.getId()).verrouille());
    }

    @Test
    void leDeblocageRemetLeCompteurAZeroSansReactiverUnCompteDesactive() {
        User u = compte(AuthService.TENTATIVES_MAX);
        u.setStatut(StatutUser.INACTIF);
        UserListResponse r = userService.deverrouiller(u.getId());
        assertEquals(0, u.getTentativesEchouees());
        assertEquals(StatutUser.INACTIF, u.getStatut());
        assertFalse(r.verrouille());
    }
}
