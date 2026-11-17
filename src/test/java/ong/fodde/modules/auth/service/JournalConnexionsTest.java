package ong.fodde.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import ong.fodde.config.JwtService;
import ong.fodde.modules.auth.dto.LoginRequest;
import ong.fodde.modules.auth.dto.LoginResponse;
import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.shared.exception.AuthentificationException;
import ong.fodde.shared.exception.CompteVerrouilleException;

/** Chaque tentative de connexion est inscrite au journal, et le journal ne bloque jamais la connexion. */
class JournalConnexionsTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final JournalConnexions journal = mock(JournalConnexions.class);
    private final AuthService service = new AuthService(users, encoder, jwt, journal);
    private static final String IP = "41.82.1.2";
    private static final String NAV = "Mozilla/5.0";

    private User compte(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPrenom("Awa");
        u.setNom("Diop");
        u.setStatut(StatutUser.ACTIF);
        u.setTentativesEchouees(0);
        u.setMotDePasseHash("hash");
        Role r = new Role();
        r.setNom("ROLE_RAF");
        u.setRoles(Set.of(r));
        when(users.findByEmail(email)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    void connexionReussieJournalisee() {
        User u = compte("raf@fodde.org");
        when(encoder.matches("bon", "hash")).thenReturn(true);
        when(jwt.generateToken(u)).thenReturn("jeton");
        LoginResponse r = service.login(new LoginRequest("raf@fodde.org", "bon"), IP, NAV);
        assertEquals("jeton", r.token());
        verify(journal).enregistrer("raf@fodde.org", u, true, null, IP, NAV);
    }

    @Test
    void mauvaisMotDePasseJournalise() {
        User u = compte("raf@fodde.org");
        when(encoder.matches("faux", "hash")).thenReturn(false);
        assertThrows(AuthentificationException.class,
                () -> service.login(new LoginRequest("raf@fodde.org", "faux"), IP, NAV));
        verify(journal).enregistrer("raf@fodde.org", u, false, JournalConnexions.MOT_DE_PASSE, IP, NAV);
    }

    @Test
    void compteInconnuJournaliseSansUtilisateur() {
        when(users.findByEmail("x@y.z")).thenReturn(Optional.empty());
        assertThrows(AuthentificationException.class,
                () -> service.login(new LoginRequest("x@y.z", "a"), IP, NAV));
        verify(journal).enregistrer(eq("x@y.z"), isNull(), eq(false), eq(JournalConnexions.COMPTE_INCONNU), eq(IP), eq(NAV));
    }

    @Test
    void compteInactifJournalise() {
        User u = compte("old@fodde.org");
        u.setStatut(StatutUser.INACTIF);
        assertThrows(AuthentificationException.class,
                () -> service.login(new LoginRequest("old@fodde.org", "a"), IP, NAV));
        verify(journal).enregistrer("old@fodde.org", u, false, JournalConnexions.COMPTE_INACTIF, IP, NAV);
    }

    @Test
    void compteVerrouilleJournalise() {
        User u = compte("lock@fodde.org");
        u.setTentativesEchouees(5);
        assertThrows(CompteVerrouilleException.class,
                () -> service.login(new LoginRequest("lock@fodde.org", "a"), IP, NAV));
        verify(journal).enregistrer("lock@fodde.org", u, false, JournalConnexions.COMPTE_VERROUILLE, IP, NAV);
    }

    @Test
    void unePanneDuJournalNEmpechePasLaConnexion() {
        User u = compte("raf@fodde.org");
        when(encoder.matches("bon", "hash")).thenReturn(true);
        when(jwt.generateToken(u)).thenReturn("jeton");
        doThrow(new RuntimeException("base indisponible")).when(journal)
                .enregistrer(anyString(), any(), anyBoolean(), any(), any(), any());
        assertEquals("jeton", service.login(new LoginRequest("raf@fodde.org", "bon"), IP, NAV).token());
    }

    @Test
    void lesTextesTropLongsSontTronques() {
        assertEquals(300, JournalConnexions.tronquer("a".repeat(500), 300).length());
        assertEquals(null, JournalConnexions.tronquer(null, 10));
    }
}
