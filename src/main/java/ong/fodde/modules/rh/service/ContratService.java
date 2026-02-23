package ong.fodde.modules.rh.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.rh.dto.ContratRequest;
import ong.fodde.modules.rh.dto.ContratResponse;
import ong.fodde.modules.rh.entity.Contrat;
import ong.fodde.modules.rh.entity.StatutContrat;
import ong.fodde.modules.rh.repository.ContratRepository;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@Transactional
public class ContratService {

    private final ContratRepository contratRepository;

    public ContratService(ContratRepository contratRepository) {
        this.contratRepository = contratRepository;
    }

    public ContratResponse creerContrat(ContratRequest request) {
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
