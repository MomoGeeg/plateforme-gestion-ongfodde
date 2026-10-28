package ong.fodde.modules.acquisitions.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.acquisitions.dto.AcquisitionRequest;
import ong.fodde.modules.acquisitions.dto.AcquisitionResponse;
import ong.fodde.modules.acquisitions.entity.Acquisition;
import ong.fodde.modules.acquisitions.entity.StatutAcquisition;
import ong.fodde.modules.acquisitions.repository.AcquisitionRepository;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class AcquisitionService {

    private final AcquisitionRepository acquisitionRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;

    public AcquisitionService(AcquisitionRepository acquisitionRepository,
                               UserRepository userRepository,
                               ProjetRepository projetRepository) {
        this.acquisitionRepository = acquisitionRepository;
        this.userRepository = userRepository;
        this.projetRepository = projetRepository;
    }

    public AcquisitionResponse creerAcquisition(AcquisitionRequest request, UUID soumisParId) {
        Acquisition acq = new Acquisition();
        acq.setCodeActif(request.codeActif());
        acq.setDesignation(request.designation());
        acq.setJustification(request.justification());
        acq.setCategorie(request.categorie());
        acq.setProjetId(request.projetId());
        acq.setValeur(request.valeur());
        acq.setDateAcquisition(request.dateAcquisition());
        acq.setSoumisParId(soumisParId);
        acq.setStatut(StatutAcquisition.EN_ATTENTE);

        return toResponse(acquisitionRepository.save(acq));
    }

    @Transactional(readOnly = true)
    public List<AcquisitionResponse> listerToutesAcquisitions() {
        return acquisitionRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AcquisitionResponse validerAcquisition(UUID id, UUID userId) {
        Acquisition acq = getOrThrow(id);
        if (acq.getStatut() != StatutAcquisition.EN_ATTENTE) {
            throw new BusinessException("Seule une acquisition EN_ATTENTE peut être validée (statut : " + acq.getStatut() + ")");
        }
        acq.setStatut(StatutAcquisition.VALIDEE);
        return toResponse(acquisitionRepository.save(acq));
    }

    public AcquisitionResponse rejeterAcquisition(UUID id, String motif, UUID userId) {
        Acquisition acq = getOrThrow(id);
        if (acq.getStatut() != StatutAcquisition.EN_ATTENTE) {
            throw new BusinessException("Seule une acquisition EN_ATTENTE peut être rejetée (statut : " + acq.getStatut() + ")");
        }
        if (motif == null || motif.isBlank()) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        acq.setStatut(StatutAcquisition.REJETEE);
        acq.setMotifRejet(motif);
        return toResponse(acquisitionRepository.save(acq));
    }

    private Acquisition getOrThrow(UUID id) {
        return acquisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acquisition introuvable : " + id));
    }

    private AcquisitionResponse toResponse(Acquisition a) {
        String projetCode = projetRepository.findById(a.getProjetId())
                .map(p -> p.getCode())
                .orElse(null);

        String soumisPar = a.getSoumisParId() != null
                ? userRepository.findById(a.getSoumisParId())
                        .map(u -> u.getPrenom() + " " + u.getNom())
                        .orElse(null)
                : null;

        return new AcquisitionResponse(
                a.getId(),
                a.getCodeActif(),
                a.getDesignation(),
                a.getJustification(),
                a.getCategorie(),
                a.getProjetId(),
                projetCode,
                a.getValeur(),
                a.getDateAcquisition(),
                a.getSoumisParId(),
                soumisPar,
                a.getStatut(),
                a.getMotifRejet(),
                a.getCreatedAt()
        );
    }
}
