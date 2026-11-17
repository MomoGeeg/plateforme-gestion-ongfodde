package ong.fodde.modules.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ong.fodde.modules.auth.repository.ConnexionRepository;

/** Suppression de l'historique des connexions par ancienneté. */
class SuppressionHistoriqueTest {

    private final ConnexionRepository repo = mock(ConnexionRepository.class);
    private final JournalConnexions journal = new JournalConnexions(repo, 86_400_000L);

    private LocalDateTime limiteUtilisee(int jours) {
        when(repo.supprimerAvant(any())).thenReturn(7);
        assertEquals(7, journal.supprimer(jours, UUID.randomUUID()));
        ArgumentCaptor<LocalDateTime> limite = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repo).supprimerAvant(limite.capture());
        return limite.getValue();
    }

    @Test
    void plusDeTrenteJours() {
        LocalDateTime limite = limiteUtilisee(30);
        long ecart = Duration.between(limite, LocalDateTime.now().minusDays(30)).abs().toSeconds();
        assertTrue(ecart < 5, "limite à 30 jours");
    }

    @Test
    void zeroSupprimeTout() {
        assertTrue(limiteUtilisee(0).isAfter(LocalDateTime.now()), "limite dans le futur : tout est supprimé");
    }

    @Test
    void nombreDeJoursNegatifRefuse() {
        assertThrows(IllegalArgumentException.class, () -> journal.supprimer(-1, UUID.randomUUID()));
    }
}
