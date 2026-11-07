package ong.fodde.modules.historique.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.historique.entity.HistoriqueDemande;
import ong.fodde.modules.historique.entity.TypeDemande;
import ong.fodde.modules.historique.repository.HistoriqueDemandeRepository;

/**
 * Journal des changements de statut des demandes. Chaque service métier
 * appelle {@link #enregistrer} dans la même transaction que le changement :
 * si l'un échoue, l'autre est annulé.
 */
@Service
@Transactional
public class HistoriqueService {

    private final HistoriqueDemandeRepository repository;
    private final UserRepository userRepository;

    public HistoriqueService(HistoriqueDemandeRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    /** Enregistre une étape. {@code statutAvant} vide pour la création. */
    public void enregistrer(TypeDemande type, UUID demandeId, Enum<?> statutAvant, Enum<?> statutApres,
                            UUID auteurId, String motif) {
        HistoriqueDemande h = new HistoriqueDemande();
        h.setTypeDemande(type);
        h.setDemandeId(demandeId);
        h.setStatutAvant(statutAvant != null ? statutAvant.name() : null);
        h.setStatutApres(statutApres.name());
        h.setAuteurId(auteurId);
        h.setAuteurNom(nomUtilisateur(auteurId));
        h.setMotif(motif != null && !motif.isBlank() ? motif : null);
        h.setDateAction(LocalDateTime.now());
        repository.save(h);
    }

    /**
     * Historique d'une demande, du plus ancien au plus récent.
     *
     * @param creation  étape de création déduite de la demande, ajoutée en tête si le
     *                  journal ne la contient pas (demande antérieure à l'historique)
     * @param anciennes étapes déduites des champs de la demande, utilisées seulement
     *                  si le journal ne contient aucun changement de statut
     */
    @Transactional(readOnly = true)
    public List<EtapeHistorique> historique(TypeDemande type, UUID demandeId,
                                            EtapeHistorique creation, List<EtapeHistorique> anciennes) {
        List<HistoriqueDemande> journal = repository.findByTypeDemandeAndDemandeIdOrderByDateActionAsc(type, demandeId);
        return assembler(journal, creation, anciennes);
    }

    /** Assemblage pur (testable sans base) du journal et des étapes reconstituées. */
    static List<EtapeHistorique> assembler(List<HistoriqueDemande> journal,
                                           EtapeHistorique creation, List<EtapeHistorique> anciennes) {
        List<EtapeHistorique> etapes = new ArrayList<>();
        boolean creationJournalisee = journal.stream().anyMatch(h -> h.getStatutAvant() == null);
        if (!creationJournalisee && creation != null) {
            etapes.add(creation);
        }
        boolean changementJournalise = journal.stream().anyMatch(h -> h.getStatutAvant() != null);
        if (!changementJournalise && anciennes != null) {
            etapes.addAll(anciennes);
        }
        journal.forEach(h -> etapes.add(new EtapeHistorique(
                h.getStatutAvant(), h.getStatutApres(), h.getAuteurId(), h.getAuteurNom(),
                h.getMotif(), h.getDateAction(), false)));
        return etapes;
    }

    /** Étape déduite des champs d'une demande (sans journal). */
    public EtapeHistorique reconstituer(Enum<?> statutAvant, Enum<?> statutApres, UUID auteurId,
                                        String motif, LocalDateTime date) {
        return new EtapeHistorique(
                statutAvant != null ? statutAvant.name() : null,
                statutApres.name(),
                auteurId,
                nomUtilisateur(auteurId),
                motif,
                date,
                true);
    }

    private String nomUtilisateur(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse(null);
    }
}
