package ong.fodde.modules.financier.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.dto.AvanceRequest;
import ong.fodde.modules.financier.dto.AvanceResponse;
import ong.fodde.modules.financier.entity.Avance;
import ong.fodde.modules.financier.entity.StatutAvance;
import ong.fodde.modules.financier.repository.AvanceRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class AvanceService {

    private final AvanceRepository avanceRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;

    public AvanceService(AvanceRepository avanceRepository,
                         UserRepository userRepository,
                         ProjetRepository projetRepository) {
        this.avanceRepository = avanceRepository;
        this.userRepository = userRepository;
        this.projetRepository = projetRepository;
    }

    public AvanceResponse creerAvance(AvanceRequest request) {
        Avance avance = new Avance();
        avance.setBeneficiaireId(request.beneficiaireId());
        avance.setProjetId(request.projetId());
        avance.setMontant(request.montant());
        avance.setMotif(request.motif());
        avance.setDateOctroi(request.dateOctroi() != null ? request.dateOctroi() : LocalDate.now());
        avance.setStatut(StatutAvance.EN_COURS);

        Avance saved = avanceRepository.save(avance);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AvanceResponse> listerAvancesProjet(UUID projetId) {
        return avanceRepository.findByProjetId(projetId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvanceResponse> listerAvancesBeneficiaire(UUID beneficiaireId) {
        return avanceRepository.findByBeneficiaireId(beneficiaireId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Liste toutes les avances. Si justifiee=false → exclut les SOLDEE. */
    @Transactional(readOnly = true)
    public List<AvanceResponse> listerToutesAvances(Boolean justifiee) {
        return avanceRepository.findAll().stream()
                .filter(a -> justifiee == null || Boolean.TRUE.equals(justifiee)
                        ? true
                        : a.getStatut() != StatutAvance.SOLDEE)
                .map(this::toResponse)
                .toList();
    }

    public AvanceResponse justifierAvance(UUID id, BigDecimal montantJustifie, LocalDate dateJustification) {
        Avance avance = avanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avance non trouvée avec l'id : " + id));

        avance.setMontantJustifie(montantJustifie);
        avance.setDateJustification(dateJustification != null ? dateJustification : LocalDate.now());

        if (montantJustifie.compareTo(avance.getMontant()) >= 0) {
            avance.setStatut(StatutAvance.SOLDEE);
        } else {
            avance.setStatut(StatutAvance.PARTIELLEMENT_JUSTIFIEE);
        }

        Avance saved = avanceRepository.save(avance);
        return toResponse(saved);
    }

    private AvanceResponse toResponse(Avance a) {
        String beneficiaireNom = userRepository.findById(a.getBeneficiaireId())
                .map(u -> u.getPrenom() + " " + u.getNom())
                .orElse(null);

        String projetCode = projetRepository.findById(a.getProjetId())
                .map(p -> p.getCode())
                .orElse(null);

        long joursEcoules = a.getDateOctroi() != null
                ? ChronoUnit.DAYS.between(a.getDateOctroi(), LocalDate.now())
                : 0;

        return new AvanceResponse(
                a.getId(),
                a.getBeneficiaireId(),
                beneficiaireNom,
                a.getProjetId(),
                projetCode,
                a.getMontant(),
                a.getMotif(),
                a.getStatut(),
                a.getDateOctroi(),
                a.getDateJustification(),
                a.getMontantJustifie(),
                joursEcoules,
                a.getCreatedAt()
        );
    }
}
