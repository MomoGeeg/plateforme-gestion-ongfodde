package ong.fodde.modules.financier.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.financier.repository.DepenseRepository;
import ong.fodde.modules.financier.repository.LigneBudgetaireRepository;
import ong.fodde.modules.financier.repository.PieceJustificativeRepository;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.securite.AccesPersonnel;
import ong.fodde.shared.exception.BusinessException;

/** Règle : au-delà du seuil, une dépense ne peut être soumise sans pièce justificative. */
class DepenseServicePieceTest {

    private final DepenseRepository depenses = mock(DepenseRepository.class);
    private final PieceJustificativeRepository pieces = mock(PieceJustificativeRepository.class);
    private final DepenseService service = new DepenseService(depenses, mock(LigneBudgetaireRepository.class),
        mock(ProjetRepository.class), mock(UserRepository.class), pieces,
        mock(HistoriqueService.class), mock(AccesPersonnel.class), new BigDecimal("100000"));

    private UUID brouillon(String montant) {
        Depense d = new Depense();
        UUID id = UUID.randomUUID();
        d.setId(id);
        d.setMontant(new BigDecimal(montant));
        d.setStatut(StatutDepense.BROUILLON);
        d.setLigneBudgetaireId(UUID.randomUUID());
        d.setProjetId(UUID.randomUUID());
        d.setSaisiParId(UUID.randomUUID());
        when(depenses.findById(id)).thenReturn(Optional.of(d));
        when(depenses.save(any())).thenAnswer(i -> i.getArgument(0));
        return id;
    }

    @Test
    void auDelaDuSeuilSansPieceLaSoumissionEstRefusee() {
        UUID id = brouillon("250000");
        when(pieces.countByDepenseId(id)).thenReturn(0L);
        assertThrows(BusinessException.class, () -> service.changerStatut(id, StatutDepense.SOUMISE, UUID.randomUUID(), null));
    }

    @Test
    void auDelaDuSeuilAvecPieceLaSoumissionPasse() {
        UUID id = brouillon("250000");
        when(pieces.countByDepenseId(id)).thenReturn(1L);
        assertDoesNotThrow(() -> service.changerStatut(id, StatutDepense.SOUMISE, UUID.randomUUID(), null));
    }

    @Test
    void sousLeSeuilLaPieceNEstPasExigee() {
        UUID id = brouillon("100000");
        when(pieces.countByDepenseId(id)).thenReturn(0L);
        assertDoesNotThrow(() -> service.changerStatut(id, StatutDepense.SOUMISE, UUID.randomUUID(), null));
    }

    @Test
    void lAnnulationDUnBrouillonNExigePasDePiece() {
        UUID id = brouillon("900000");
        when(pieces.countByDepenseId(id)).thenReturn(0L);
        assertDoesNotThrow(() -> service.changerStatut(id, StatutDepense.ANNULEE, UUID.randomUUID(), null));
    }
}
