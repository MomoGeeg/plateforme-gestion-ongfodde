package ong.fodde.modules.rh.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.rh.dto.ContratRequest;
import ong.fodde.modules.rh.dto.ContratResponse;
import ong.fodde.modules.rh.entity.Contrat;
import ong.fodde.modules.rh.entity.StatutContrat;
import ong.fodde.modules.rh.entity.TypeContrat;
import ong.fodde.modules.rh.repository.ContratRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class ContratService {

    private final ContratRepository contratRepository;

    public ContratService(ContratRepository contratRepository) {
        this.contratRepository = contratRepository;
    }

    public ContratResponse creerContrat(ContratRequest request) {

        // Règle 1 : Date de fin obligatoire pour CDD, Consultant, Stagiaire
        if (request.typeContrat() == TypeContrat.CDD
                || request.typeContrat() == TypeContrat.CONSULTANT
                || request.typeContrat() == TypeContrat.STAGE) {
            if (request.dateFin() == null) {
                throw new BusinessException(
                    "La date de fin est obligatoire pour un contrat de type " + request.typeContrat()
                );
            }
        }

        // Règle 2 : Un employé ne peut avoir qu'un seul contrat actif à la fois
        contratRepository.findByEmployeIdAndStatut(request.employeId(), StatutContrat.EN_COURS)
            .ifPresent(c -> {
                throw new BusinessException(
                    "L'employé possède déjà un contrat actif (id : " + c.getId() + "). "
                    + "Veuillez résilier le contrat en cours avant d'en créer un nouveau."
                );
            });

        // Règle 3 : Date de fin doit être après date de début
        if (request.dateFin() != null && request.dateFin().isBefore(request.dateDebut())) {
            throw new BusinessException("La date de fin ne peut pas être antérieure à la date de début");
        }

        Contrat contrat = new Contrat();
        contrat.setEmployeId(request.employeId());
        contrat.setTypeContrat(request.typeContrat());
        contrat.setDateDebut(request.dateDebut());
        contrat.setDateFin(request.dateFin());
        contrat.setSalaireBase(request.salaireBase());
        contrat.setDevise(request.devise());
        contrat.setPoste(request.poste());
        contrat.setDocumentUrl(request.documentUrl());
        contrat.setStatut(StatutContrat.EN_COURS);

        Contrat saved = contratRepository.save(contrat);

        // Règle 4 : Alerte immédiate si le contrat expire dans moins de 60 jours
        verifierEcheanceContrat(saved);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContratResponse> listerContratsEmploye(UUID employeId) {
        return contratRepository.findByEmployeId(employeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContratResponse contratActif(UUID employeId) {
        Contrat contrat = contratRepository.findByEmployeIdAndStatut(employeId, StatutContrat.EN_COURS)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun contrat actif trouvé pour l'employé : " + employeId));
        return toResponse(contrat);
    }

    // Règle 5 : Renouvellement de contrat
    public ContratResponse renouvelerContrat(UUID id, LocalDate nouvelleDateFin, String motif) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable : " + id));

        if (contrat.getStatut() != StatutContrat.EN_COURS
                && contrat.getStatut() != StatutContrat.EXPIRE) {
            throw new BusinessException(
                "Seul un contrat EN_COURS ou EXPIRE peut être renouvelé"
            );
        }

        if (nouvelleDateFin == null || nouvelleDateFin.isBefore(LocalDate.now())) {
            throw new BusinessException(
                "La nouvelle date de fin doit être dans le futur"
            );
        }

        if (contrat.getDateFin() != null && nouvelleDateFin.isBefore(contrat.getDateFin())) {
            throw new BusinessException(
                "La nouvelle date de fin doit être postérieure à la date de fin actuelle"
            );
        }

        contrat.setDateFin(nouvelleDateFin);
        contrat.setStatut(StatutContrat.EN_COURS);

        return toResponse(contratRepository.save(contrat));
    }

    // Règle 6 : Résiliation de contrat
    public ContratResponse resilierContrat(UUID id, String motif) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable : " + id));

        if (contrat.getStatut() == StatutContrat.RESILIE) {
            throw new BusinessException("Ce contrat est déjà résilié");
        }

        if (contrat.getStatut() == StatutContrat.EXPIRE) {
            throw new BusinessException("Impossible de résilier un contrat déjà expiré");
        }

        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif de résiliation est obligatoire");
        }

        contrat.setStatut(StatutContrat.RESILIE);

        return toResponse(contratRepository.save(contrat));
    }

    // Règle 7 : Vérification échéances — à appeler par @Scheduled
    public void verifierEcheancesContrats() {
        List<Contrat> contratsActifs = contratRepository.findByStatut(StatutContrat.EN_COURS);
        LocalDate aujourdHui = LocalDate.now();

        for (Contrat contrat : contratsActifs) {
            if (contrat.getDateFin() == null) continue;

            long joursRestants = ChronoUnit.DAYS.between(aujourdHui, contrat.getDateFin());

            if (joursRestants < 0) {
                // Contrat expiré → mettre à jour le statut
                contrat.setStatut(StatutContrat.EXPIRE);
                contratRepository.save(contrat);
            }
        }
    }

    // Vérification échéance lors de la création
    private void verifierEcheanceContrat(Contrat contrat) {
        if (contrat.getDateFin() == null) return;

        // Vérification silencieuse de l'échéance
    }

    private ContratResponse toResponse(Contrat c) {
        return new ContratResponse(
                c.getId(),
                c.getEmployeId(),
                c.getTypeContrat(),
                c.getDateDebut(),
                c.getDateFin(),
                c.getSalaireBase(),
                c.getDevise(),
                c.getPoste(),
                c.getStatut(),
                c.getDocumentUrl(),
                c.getCreatedAt()
        );
    }
}