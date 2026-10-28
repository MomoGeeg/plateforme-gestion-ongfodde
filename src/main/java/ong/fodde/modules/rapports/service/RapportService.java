package ong.fodde.modules.rapports.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.rapports.dto.RapportRequest;
import ong.fodde.modules.rapports.dto.RapportResponse;
import ong.fodde.modules.rapports.entity.Rapport;
import ong.fodde.modules.rapports.repository.RapportRepository;

@Service
@Transactional
public class RapportService {

    private final RapportRepository rapportRepository;
    private final UserRepository userRepository;

    public RapportService(RapportRepository rapportRepository,
                          UserRepository userRepository) {
        this.rapportRepository = rapportRepository;
        this.userRepository = userRepository;
    }

    public RapportResponse genererRapport(RapportRequest request, UUID genereParId) {
        Rapport rapport = new Rapport();
        rapport.setType(request.type());
        rapport.setTypeLabel(request.typeLabel());
        rapport.setPeriode(request.periode());
        rapport.setProjetCode(request.projetCode() != null ? request.projetCode() : "Tous projets");
        rapport.setBailleur(request.bailleur());
        rapport.setFormat(request.format() != null ? request.format() : "PDF");
        rapport.setArchive(true);
        rapport.setGenereParId(genereParId);

        if (genereParId != null) {
            String nom = userRepository.findById(genereParId)
                    .map(u -> u.getPrenom() + " " + u.getNom())
                    .orElse(null);
            rapport.setGenerePar(nom);
        }

        return toResponse(rapportRepository.save(rapport));
    }

    @Transactional(readOnly = true)
    public List<RapportResponse> listerRapports() {
        return rapportRepository.findAllByOrderByDateGenerationDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    private RapportResponse toResponse(Rapport r) {
        return new RapportResponse(
                r.getId(),
                r.getType(),
                r.getTypeLabel(),
                r.getPeriode(),
                r.getProjetCode(),
                r.getBailleur(),
                r.getFormat(),
                "—",   // taille fichier non calculée (génération réelle hors scope)
                r.getArchive(),
                r.getGenerePar(),
                r.getDateGeneration()
        );
    }
}
