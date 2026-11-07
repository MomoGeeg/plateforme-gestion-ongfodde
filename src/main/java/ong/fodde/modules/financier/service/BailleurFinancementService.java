package ong.fodde.modules.financier.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.financier.dto.BailleurRequest;
import ong.fodde.modules.financier.dto.BailleurResponse;
import ong.fodde.modules.financier.dto.FinancementRequest;
import ong.fodde.modules.financier.dto.FinancementResponse;
import ong.fodde.modules.financier.entity.Financement;
import ong.fodde.modules.financier.repository.FinancementRepository;
import ong.fodde.modules.projets.entity.Bailleur;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.repository.BailleurRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

/**
 * Bailleurs de fonds et financements accordés aux projets (une convention =
 * un financement d'un bailleur pour un projet, avec son montant et sa période).
 */
@Service
@Transactional
public class BailleurFinancementService {

    private final BailleurRepository bailleurRepository;
    private final FinancementRepository financementRepository;
    private final ProjetRepository projetRepository;

    public BailleurFinancementService(BailleurRepository bailleurRepository,
                                      FinancementRepository financementRepository,
                                      ProjetRepository projetRepository) {
        this.bailleurRepository = bailleurRepository;
        this.financementRepository = financementRepository;
        this.projetRepository = projetRepository;
    }

    // ── Bailleurs ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BailleurResponse> listerBailleurs() {
        return bailleurRepository.findAll().stream()
            .sorted(Comparator.comparing(Bailleur::getNom, String.CASE_INSENSITIVE_ORDER))
            .map(this::versReponse)
            .toList();
    }

    public BailleurResponse creerBailleur(BailleurRequest request) {
        String nom = request.nom().trim();
        if (bailleurRepository.findByNom(nom).isPresent()) {
            throw new BusinessException("Un bailleur nommé « " + nom + " » existe déjà");
        }
        Bailleur b = new Bailleur();
        b.setNom(nom);
        b.setSigle(vide(request.sigle()));
        b.setPays(vide(request.pays()));
        b.setContact(vide(request.contact()));
        b.setEmail(vide(request.email()));
        return versReponse(bailleurRepository.save(b));
    }

    // ── Financements ────────────────────────────────────────────────────────

    /** Financements, filtrés par projet et/ou par bailleur (les deux sont facultatifs). */
    @Transactional(readOnly = true)
    public List<FinancementResponse> listerFinancements(UUID projetId, UUID bailleurId) {
        List<Financement> liste = projetId != null
            ? financementRepository.findByProjetId(projetId)
            : bailleurId != null
                ? financementRepository.findByBailleurId(bailleurId)
                : financementRepository.findAll();
        if (projetId != null && bailleurId != null) {
            liste = liste.stream().filter(f -> f.getBailleurId().equals(bailleurId)).toList();
        }
        return versReponses(liste);
    }

    public FinancementResponse creerFinancement(FinancementRequest request, UUID auteurId) {
        bailleurRepository.findById(request.bailleurId())
            .orElseThrow(() -> new ResourceNotFoundException("Bailleur introuvable : " + request.bailleurId()));
        projetRepository.findById(request.projetId())
            .orElseThrow(() -> new ResourceNotFoundException("Projet introuvable : " + request.projetId()));
        if (request.dateDebut() != null && request.dateFin() != null && request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("La fin de la convention doit suivre son début");
        }
        Financement f = new Financement();
        f.setBailleurId(request.bailleurId());
        f.setProjetId(request.projetId());
        f.setReferenceConvention(request.referenceConvention().trim());
        f.setMontantAccorde(request.montantAccorde());
        f.setDevise(request.devise());
        f.setDateDebut(request.dateDebut());
        f.setDateFin(request.dateFin());
        f.setConditionsReporting(vide(request.conditionsReporting()));
        f.setCreatedBy(auteurId);
        return versReponses(List.of(financementRepository.save(f))).get(0);
    }

    // ── Conversions ─────────────────────────────────────────────────────────

    private BailleurResponse versReponse(Bailleur b) {
        return new BailleurResponse(b.getId(), b.getNom(), b.getSigle(), b.getPays(), b.getContact(), b.getEmail());
    }

    /** Charge bailleurs et projets en une requête chacun, puis convertit. */
    private List<FinancementResponse> versReponses(List<Financement> financements) {
        Map<UUID, Bailleur> bailleurs = bailleurRepository.findAllById(
                financements.stream().map(Financement::getBailleurId).distinct().toList())
            .stream().collect(Collectors.toMap(Bailleur::getId, Function.identity()));
        Map<UUID, Projet> projets = projetRepository.findAllById(
                financements.stream().map(Financement::getProjetId).distinct().toList())
            .stream().collect(Collectors.toMap(Projet::getId, Function.identity()));
        return financements.stream()
            .sorted(Comparator.comparing(Financement::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(f -> {
                Bailleur b = bailleurs.get(f.getBailleurId());
                Projet p = projets.get(f.getProjetId());
                return new FinancementResponse(
                    f.getId(),
                    f.getBailleurId(),
                    b != null ? b.getNom() : null,
                    b != null ? b.getSigle() : null,
                    f.getProjetId(),
                    p != null ? p.getCode() : null,
                    p != null ? p.getTitre() : null,
                    f.getReferenceConvention(),
                    f.getMontantAccorde(),
                    f.getDevise(),
                    f.getDateDebut(),
                    f.getDateFin(),
                    f.getConditionsReporting(),
                    f.getCreatedAt());
            })
            .toList();
    }

    private static String vide(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
