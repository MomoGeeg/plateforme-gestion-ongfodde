package ong.fodde.modules.logistique.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ong.fodde.modules.logistique.dto.BienRequest;
import ong.fodde.modules.logistique.dto.BienResponse;
import ong.fodde.modules.logistique.dto.MouvementRequest;
import ong.fodde.modules.logistique.dto.MouvementResponse;
import ong.fodde.modules.logistique.dto.VehiculeRequest;
import ong.fodde.modules.logistique.dto.VehiculeResponse;
import ong.fodde.modules.logistique.entity.Bien;
import ong.fodde.modules.logistique.entity.EtatBien;
import ong.fodde.modules.logistique.entity.MouvementMateriel;
import ong.fodde.modules.logistique.entity.Vehicule;
import ong.fodde.modules.logistique.repository.BienRepository;
import ong.fodde.modules.logistique.repository.MouvementMaterielRepository;
import ong.fodde.modules.logistique.repository.VehiculeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class LogistiqueService {

    private static final int SEUIL_ALERTE_JOURS = 30;
    private final AtomicInteger codeCounter = new AtomicInteger();

    private final BienRepository bienRepository;
    private final VehiculeRepository vehiculeRepository;
    private final MouvementMaterielRepository mouvementRepository;

    // ══════════════════════════ BIENS ══════════════════════════

    public List<BienResponse> listerBiens() {
        return bienRepository.findByIsDeletedFalseOrderByCreatedAtDesc()
                .stream().map(this::toBienResponse).toList();
    }

    public BienResponse getBien(UUID id) {
        return toBienResponse(findBien(id));
    }

    @Transactional
    public BienResponse creerBien(BienRequest req) {
        Bien bien = new Bien();
        bien.setCodeActif(genererCodeActif());
        mapBien(bien, req);
        return toBienResponse(bienRepository.save(bien));
    }

    @Transactional
    public BienResponse modifierBien(UUID id, BienRequest req) {
        Bien bien = findBien(id);
        mapBien(bien, req);
        return toBienResponse(bienRepository.save(bien));
    }

    /**
     * Marque un bien comme HORS_SERVICE (pas de suppression physique).
     */
    @Transactional
    public void mettreHorsService(UUID id) {
        Bien bien = findBien(id);
        bien.setEtat(EtatBien.HORS_SERVICE);
        bienRepository.save(bien);
    }

    // ══════════════════════════ VEHICULES ══════════════════════

    public List<VehiculeResponse> listerVehicules() {
        return vehiculeRepository.findByIsDeletedFalseOrderByCreatedAtDesc()
                .stream().map(this::toVehiculeResponse).toList();
    }

    public VehiculeResponse getVehicule(UUID id) {
        return toVehiculeResponse(findVehicule(id));
    }

    @Transactional
    public VehiculeResponse creerVehicule(VehiculeRequest req) {
        if (vehiculeRepository.existsByImmatriculationAndIsDeletedFalse(req.immatriculation())) {
            throw new BusinessException("Un véhicule avec cette immatriculation existe déjà : " + req.immatriculation());
        }
        Vehicule v = new Vehicule();
        mapVehicule(v, req);
        return toVehiculeResponse(vehiculeRepository.save(v));
    }

    @Transactional
    public VehiculeResponse modifierVehicule(UUID id, VehiculeRequest req) {
        Vehicule v = findVehicule(id);
        mapVehicule(v, req);
        return toVehiculeResponse(vehiculeRepository.save(v));
    }

    public List<VehiculeResponse> getAlertes() {
        LocalDate seuil = LocalDate.now().plusDays(SEUIL_ALERTE_JOURS);
        List<Vehicule> assurances = vehiculeRepository.findAssurancesExpirantAvant(seuil);
        List<Vehicule> visites    = vehiculeRepository.findVisitesTechniquesExpirantAvant(seuil);
        List<Vehicule> revision   = vehiculeRepository.findVehiculesRevisionRequise();

        return java.util.stream.Stream.of(assurances, visites, revision)
                .flatMap(List::stream)
                .distinct()
                .map(this::toVehiculeResponse)
                .toList();
    }

    // ══════════════════════════ MOUVEMENTS ═════════════════════

    public List<MouvementResponse> listerMouvements() {
        return mouvementRepository.findByIsDeletedFalseOrderByDateMouvementDesc()
                .stream().map(this::toMouvementResponse).toList();
    }

    public List<MouvementResponse> getMouvementsBien(UUID bienId) {
        return mouvementRepository.findByBienIdAndIsDeletedFalseOrderByDateMouvementDesc(bienId)
                .stream().map(this::toMouvementResponse).toList();
    }

    public List<MouvementResponse> getMouvementsVehicule(UUID vehiculeId) {
        return mouvementRepository.findByVehiculeIdAndIsDeletedFalseOrderByDateMouvementDesc(vehiculeId)
                .stream().map(this::toMouvementResponse).toList();
    }

    @Transactional
    public MouvementResponse enregistrerMouvement(MouvementRequest req) {
        if (req.bienId() == null && req.vehiculeId() == null) {
            throw new BusinessException("Un mouvement doit concerner un bien ou un véhicule.");
        }

        MouvementMateriel m = new MouvementMateriel();
        m.setType(req.type());
        m.setBienId(req.bienId());
        m.setVehiculeId(req.vehiculeId());
        m.setProjetId(req.projetId());
        m.setEmployeId(req.employeId());
        m.setLocalisationDepart(req.localisationDepart());
        m.setLocalisationArrivee(req.localisationArrivee());
        m.setDateMouvement(req.dateMouvement());
        m.setMotif(req.motif());
        m.setNotes(req.notes());

        // Mettre à jour la localisation du bien si nécessaire
        if (req.bienId() != null && req.localisationArrivee() != null) {
            Bien bien = findBien(req.bienId());
            bien.setLocalisationActuelle(req.localisationArrivee());
            bienRepository.save(bien);
        }

        return toMouvementResponse(mouvementRepository.save(m));
    }

    // ══════════════════════════ KPI ════════════════════════════

    public java.util.Map<String, Object> getKpi() {
        long totalBiens = bienRepository.countByIsDeletedFalse();
        long biensHorsService = bienRepository.countByEtatAndIsDeletedFalse(EtatBien.HORS_SERVICE);
        long totalVehicules = vehiculeRepository.countByIsDeletedFalse();

        LocalDate seuil = LocalDate.now().plusDays(SEUIL_ALERTE_JOURS);
        long alertesAssurance = vehiculeRepository.findAssurancesExpirantAvant(seuil).size();
        long alertesVisite    = vehiculeRepository.findVisitesTechniquesExpirantAvant(seuil).size();
        long alertesKm        = vehiculeRepository.findVehiculesRevisionRequise().size();
        long totalAlertes     = alertesAssurance + alertesVisite + alertesKm;

        return java.util.Map.of(
            "totalBiens", totalBiens,
            "biensActifs", totalBiens - biensHorsService,
            "biensHorsService", biensHorsService,
            "totalVehicules", totalVehicules,
            "totalAlertes", totalAlertes
        );
    }

    // ══════════════════════════ PRIVÉ ══════════════════════════

    private Bien findBien(UUID id) {
        return bienRepository.findById(id)
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Bien introuvable : " + id));
    }

    private Vehicule findVehicule(UUID id) {
        return vehiculeRepository.findById(id)
                .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable : " + id));
    }

    private void mapBien(Bien bien, BienRequest req) {
        bien.setDesignation(req.designation());
        bien.setCategorie(req.categorie());
        bien.setMarque(req.marque());
        bien.setModele(req.modele());
        bien.setNumeroSerie(req.numeroSerie());
        if (req.etat() != null) bien.setEtat(req.etat());
        bien.setValeurAcquisition(req.valeurAcquisition());
        bien.setDateAcquisition(req.dateAcquisition());
        bien.setLocalisationActuelle(req.localisationActuelle());
        bien.setProjetId(req.projetId());
        bien.setResponsableId(req.responsableId());
        bien.setNotes(req.notes());
    }

    private void mapVehicule(Vehicule v, VehiculeRequest req) {
        v.setImmatriculation(req.immatriculation());
        v.setMarque(req.marque());
        v.setModele(req.modele());
        v.setAnnee(req.annee());
        if (req.kilometrageActuel() != null) v.setKilometrageActuel(req.kilometrageActuel());
        v.setKilometrageRevision(req.kilometrageRevision());
        if (req.etat() != null) v.setEtat(req.etat());
        v.setProjetId(req.projetId());
        v.setConducteurHabituelId(req.conducteurHabituelId());
        if (req.typeCarburant() != null) v.setTypeCarburant(req.typeCarburant());
        v.setDateExpirationAssurance(req.dateExpirationAssurance());
        v.setDateVisiteTechnique(req.dateVisiteTechnique());
        v.setNotes(req.notes());
    }

    private String genererCodeActif() {
        int annee = LocalDate.now().getYear();
        String base = "MAT-" + annee + "-";
        // Simple séquence incrémentale — chercher le plus grand existant
        return bienRepository.findAll().stream()
                .map(Bien::getCodeActif)
                .filter(c -> c != null && c.startsWith(base))
                .mapToInt(c -> {
                    try { return Integer.parseInt(c.substring(base.length())); }
                    catch (NumberFormatException e) { return 0; }
                })
                .max()
                .stream()
                .mapToObj(max -> base + String.format("%03d", max + 1))
                .findFirst()
                .orElse(base + "001");
    }

    private BienResponse toBienResponse(Bien b) {
        return new BienResponse(
            b.getId(), b.getCodeActif(), b.getDesignation(), b.getCategorie(),
            b.getMarque(), b.getModele(), b.getNumeroSerie(), b.getEtat(),
            b.getValeurAcquisition(), b.getDateAcquisition(), b.getLocalisationActuelle(),
            b.getProjetId(), b.getResponsableId(), b.getNotes(), b.getCreatedAt()
        );
    }

    private VehiculeResponse toVehiculeResponse(Vehicule v) {
        LocalDate today = LocalDate.now();
        Long joursAssurance = v.getDateExpirationAssurance() != null
            ? ChronoUnit.DAYS.between(today, v.getDateExpirationAssurance()) : null;
        Long joursVisite = v.getDateVisiteTechnique() != null
            ? ChronoUnit.DAYS.between(today, v.getDateVisiteTechnique()) : null;

        boolean alerteAssurance = joursAssurance != null && joursAssurance <= SEUIL_ALERTE_JOURS;
        boolean alerteVisite    = joursVisite != null && joursVisite <= SEUIL_ALERTE_JOURS;
        boolean alerteKm        = v.getKilometrageRevision() != null
            && v.getKilometrageActuel() != null
            && v.getKilometrageActuel() >= v.getKilometrageRevision();

        return new VehiculeResponse(
            v.getId(), v.getImmatriculation(), v.getMarque(), v.getModele(), v.getAnnee(),
            v.getKilometrageActuel(), v.getKilometrageRevision(), v.getEtat(),
            v.getProjetId(), v.getConducteurHabituelId(), v.getTypeCarburant(),
            v.getDateExpirationAssurance(), v.getDateVisiteTechnique(), v.getNotes(),
            joursAssurance, joursVisite, alerteAssurance, alerteVisite, alerteKm,
            v.getCreatedAt()
        );
    }

    private MouvementResponse toMouvementResponse(MouvementMateriel m) {
        String bienDesignation = null;
        String bienCodeActif   = null;
        String vehiculeImmat   = null;

        if (m.getBienId() != null) {
            bienRepository.findById(m.getBienId()).ifPresent(b -> {
                // non-final — workaround via map
            });
            var optBien = bienRepository.findById(m.getBienId());
            if (optBien.isPresent()) {
                bienDesignation = optBien.get().getDesignation();
                bienCodeActif   = optBien.get().getCodeActif();
            }
        }
        if (m.getVehiculeId() != null) {
            var optV = vehiculeRepository.findById(m.getVehiculeId());
            if (optV.isPresent()) vehiculeImmat = optV.get().getImmatriculation();
        }

        return new MouvementResponse(
            m.getId(), m.getType(),
            m.getBienId(), bienDesignation, bienCodeActif,
            m.getVehiculeId(), vehiculeImmat,
            m.getProjetId(), m.getEmployeId(),
            m.getLocalisationDepart(), m.getLocalisationArrivee(),
            m.getDateMouvement(), m.getMotif(), m.getNotes(), m.getCreatedAt()
        );
    }
}
