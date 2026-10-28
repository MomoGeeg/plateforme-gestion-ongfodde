package ong.fodde.modules.alertes.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.alertes.dto.AlerteResponse;
import ong.fodde.modules.alertes.entity.Alerte;
import ong.fodde.modules.alertes.repository.AlerteRepository;
import ong.fodde.modules.financier.entity.Depense;
import ong.fodde.modules.financier.entity.StatutDepense;
import ong.fodde.modules.financier.repository.DepenseRepository;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final ProjetRepository projetRepository;
    private final DepenseRepository depenseRepository;

    public AlerteService(AlerteRepository alerteRepository,
                         ProjetRepository projetRepository,
                         DepenseRepository depenseRepository) {
        this.alerteRepository = alerteRepository;
        this.projetRepository = projetRepository;
        this.depenseRepository = depenseRepository;
    }

    // ── Lister toutes les alertes actives (avec regénération auto) ─────────────

    @Transactional(readOnly = false)
    public List<AlerteResponse> listerAlertes() {
        genererAlertes();
        return alerteRepository.findAllActive()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Génération automatique par règles métier ───────────────────────────────

    private void genererAlertes() {
        LocalDate today = LocalDate.now();
        LocalDate dans30j = today.plusDays(30);

        List<String> referenceKeysActives = new ArrayList<>();

        // 1. Projets EN_RETARD (ACTIF + dateFin dépassée)
        List<Projet> projetsActifs = projetRepository.findByStatut(StatutProjet.ACTIF);
        for (Projet p : projetsActifs) {
            if (p.getDateFin() == null) continue;

            if (p.getDateFin().isBefore(today)) {
                String key = "PROJET_RETARD_" + p.getId();
                upsertAlerte(key,
                        "Projet en retard : " + p.getTitre(),
                        "La date de fin prévue (" + p.getDateFin() + ") est dépassée.",
                        "CRITIQUE", "PROJETS",
                        "/projets/" + p.getId());
                referenceKeysActives.add(key);

            } else if (p.getDateFin().isBefore(dans30j)) {
                String key = "PROJET_J30_" + p.getId();
                upsertAlerte(key,
                        "Projet à échéance dans 30 j : " + p.getTitre(),
                        "La date de fin prévue est dans moins de 30 jours (" + p.getDateFin() + ").",
                        "URGENT", "PROJETS",
                        "/projets/" + p.getId());
                referenceKeysActives.add(key);
            }
        }

        // 2. Projets EN_VALIDATION
        List<Projet> projetsEnValidation = projetRepository.findByStatut(StatutProjet.EN_VALIDATION);
        for (Projet p : projetsEnValidation) {
            String key = "PROJET_VALIDATION_" + p.getId();
            upsertAlerte(key,
                    "Projet en attente de validation : " + p.getTitre(),
                    "Ce projet nécessite votre approbation avant d'être activé.",
                    "WARNING", "PROJETS",
                    "/secretariat/validations");
            referenceKeysActives.add(key);
        }

        // 3. Dépenses EN_APPROBATION_SE
        List<Depense> depensesEnApprobation = depenseRepository.findByStatut(StatutDepense.EN_APPROBATION_SE);
        for (Depense d : depensesEnApprobation) {
            String key = "DEPENSE_ATTENTE_" + d.getId();
            upsertAlerte(key,
                    "Dépense en attente d'approbation : " + d.getLibelle(),
                    "Cette dépense requiert votre approbation (montant : " + d.getMontant() + " F).",
                    "URGENT", "FINANCES",
                    "/secretariat/validations");
            referenceKeysActives.add(key);
        }

        // Soft-delete des alertes devenues obsolètes (non lues uniquement)
        Set<String> referenceKeysActivesSet = Set.copyOf(referenceKeysActives);
        List<Alerte> actives = alerteRepository.findAllActive();
        for (Alerte a : actives) {
            if (!referenceKeysActivesSet.contains(a.getReferenceKey()) && !a.isLue()) {
                a.setIsDeleted(true);
                alerteRepository.save(a);
            }
        }
    }

    /** Crée l'alerte si elle n'existe pas encore (upsert par referenceKey). */
    private void upsertAlerte(String referenceKey, String titre, String description,
                               String niveau, String module, String lien) {
        if (alerteRepository.findByReferenceKey(referenceKey).isEmpty()) {
            Alerte alerte = new Alerte();
            alerte.setReferenceKey(referenceKey);
            alerte.setTitre(titre);
            alerte.setDescription(description);
            alerte.setNiveau(niveau);
            alerte.setModule(module);
            alerte.setLien(lien);
            alerte.setLue(false);
            alerteRepository.save(alerte);
        }
    }

    // ── Marquer comme lue ─────────────────────────────────────────────────────

    public AlerteResponse marquerLue(UUID id) {
        Alerte alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte non trouvée : " + id));
        alerte.setLue(true);
        return toResponse(alerteRepository.save(alerte));
    }

    public void marquerToutesLues() {
        List<Alerte> nonLues = alerteRepository.findAllActiveNonLues();
        for (Alerte a : nonLues) {
            a.setLue(true);
        }
        alerteRepository.saveAll(nonLues);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private AlerteResponse toResponse(Alerte a) {
        return new AlerteResponse(
                a.getId(),
                a.getReferenceKey(),
                a.getTitre(),
                a.getDescription(),
                a.getNiveau(),
                a.getModule(),
                a.getLien(),
                a.isLue(),
                a.getCreatedAt()
        );
    }
}
