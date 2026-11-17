package ong.fodde.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ong.fodde.modules.auth.dto.ConnexionResponse;
import ong.fodde.modules.auth.entity.Connexion;
import ong.fodde.modules.auth.repository.ConnexionRepository;

/** Heure de déconnexion et fin de session dans l'historique des connexions. */
class DeconnexionTest {

    private static final long JOUR_MS = 86_400_000L;
    private final ConnexionRepository repo = mock(ConnexionRepository.class);
    private final JournalConnexions journal = new JournalConnexions(repo, JOUR_MS);
    private final UUID awa = UUID.randomUUID();

    private Connexion ligne(UUID userId, boolean succes, LocalDateTime date) {
        Connexion c = new Connexion();
        c.setId(UUID.randomUUID());
        c.setUserId(userId);
        c.setEmail("raf@fodde.org");
        c.setSucces(succes);
        c.setDateConnexion(date);
        when(repo.findById(c.getId())).thenReturn(Optional.of(c));
        return c;
    }

    @Test
    void laDeconnexionNoteLHeure() {
        Connexion c = ligne(awa, true, LocalDateTime.now().minusHours(2));
        journal.deconnecter(c.getId(), awa);
        assertNotNull(c.getDateDeconnexion());
    }

    @Test
    void onNeFermePasLaSessionDUnAutre() {
        Connexion c = ligne(awa, true, LocalDateTime.now().minusHours(2));
        journal.deconnecter(c.getId(), UUID.randomUUID());
        assertNull(c.getDateDeconnexion());
    }

    @Test
    void uneSessionDejaFermeeGardeSonHeure() {
        Connexion c = ligne(awa, true, LocalDateTime.now().minusHours(2));
        LocalDateTime premiere = LocalDateTime.now().minusHours(1);
        c.setDateDeconnexion(premiere);
        journal.deconnecter(c.getId(), awa);
        assertEquals(premiere, c.getDateDeconnexion());
    }

    @Test
    void uneConnexionRefuseeNEstJamaisFermee() {
        Connexion c = ligne(awa, false, LocalDateTime.now().minusHours(2));
        journal.deconnecter(c.getId(), awa);
        assertNull(c.getDateDeconnexion());
        assertNull(ConnexionResponse.de(c, JOUR_MS, LocalDateTime.now()).typeFin());
    }

    @Test
    void finDeSessionParDeconnexion() {
        LocalDateTime debut = LocalDateTime.of(2026, 11, 17, 8, 0);
        Connexion c = ligne(awa, true, debut);
        c.setDateDeconnexion(debut.plusHours(3));
        ConnexionResponse r = ConnexionResponse.de(c, JOUR_MS, debut.plusDays(5));
        assertEquals("DECONNEXION", r.typeFin());
        assertEquals(debut.plusHours(3), r.finSession());
    }

    @Test
    void finDeSessionParExpirationSansDeconnexion() {
        LocalDateTime debut = LocalDateTime.of(2026, 11, 17, 8, 0);
        Connexion c = ligne(awa, true, debut);
        ConnexionResponse r = ConnexionResponse.de(c, JOUR_MS, debut.plusDays(2));
        assertEquals("EXPIRATION", r.typeFin());
        assertEquals(debut.plusDays(1), r.finSession());
    }

    @Test
    void sessionEnCours() {
        LocalDateTime debut = LocalDateTime.of(2026, 11, 17, 8, 0);
        Connexion c = ligne(awa, true, debut);
        ConnexionResponse r = ConnexionResponse.de(c, JOUR_MS, debut.plusHours(1));
        assertNull(r.typeFin());
        assertNull(r.finSession());
    }
}
