package ong.fodde.modules.logistique.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.AlerteVehicule;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.DepartRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.PersonneResponse;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.RetourRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.SortieResponse;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.VehiculeRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.VehiculeResponse;
import ong.fodde.modules.logistique.entity.EtatMateriel;
import ong.fodde.modules.logistique.entity.SortieVehicule;
import ong.fodde.modules.logistique.entity.Vehicule;
import ong.fodde.modules.logistique.repository.SortieVehiculeRepository;
import ong.fodde.modules.logistique.repository.VehiculeRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;

import static ong.fodde.shared.util.Prechargement.precharger;

/**
 * Parc de véhicules et carnet de bord. Le kilométrage du véhicule avance au
 * retour de chaque sortie ; une seule sortie peut être en cours par véhicule.
 */
@Service
@Transactional
public class VehiculeService {

    /** Délai d'alerte avant l'échéance de l'assurance ou de la visite technique. */
    static final int JOURS_ALERTE = 30;
    /** Distance d'alerte avant la révision. */
    static final int KM_ALERTE_REVISION = 500;
    private static final java.time.format.DateTimeFormatter JJ_MM_AAAA = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final VehiculeRepository vehiculeRepository;
    private final SortieVehiculeRepository sortieRepository;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    public VehiculeService(VehiculeRepository vehiculeRepository, SortieVehiculeRepository sortieRepository,
                           ProjetRepository projetRepository, UserRepository userRepository) {
        this.vehiculeRepository = vehiculeRepository;
        this.sortieRepository = sortieRepository;
        this.projetRepository = projetRepository;
        this.userRepository = userRepository;
    }

    // ── Véhicules ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<VehiculeResponse> lister() {
        List<Vehicule> vehicules = vehiculeRepository.findAllByOrderByImmatriculationAsc();
        Map<UUID, UUID> enCours = sortieRepository.findByDateRetourIsNull().stream()
                .collect(Collectors.toMap(SortieVehicule::getVehiculeId, SortieVehicule::getId, (a, b) -> a));
        precharger(projetRepository, vehicules, Vehicule::getProjetId);
        precharger(userRepository, vehicules, Vehicule::getConducteurHabituelId);
        LocalDate aujourdhui = LocalDate.now();
        return vehicules.stream().map(v -> toResponse(v, enCours.get(v.getId()), aujourdhui)).toList();
    }

    @Transactional(readOnly = true)
    public VehiculeResponse obtenir(UUID id) {
        Vehicule v = trouver(id);
        UUID enCours = sortieRepository.findFirstByVehiculeIdAndDateRetourIsNull(id).map(SortieVehicule::getId).orElse(null);
        return toResponse(v, enCours, LocalDate.now());
    }

    public VehiculeResponse creer(VehiculeRequest req, UUID auteurId) {
        String immat = normaliser(req.immatriculation());
        if (vehiculeRepository.existsByImmatriculationIgnoreCase(immat)) {
            throw new BusinessException("Un véhicule immatriculé " + immat + " existe déjà");
        }
        Vehicule v = new Vehicule();
        v.setImmatriculation(immat);
        v.setKilometrageActuel(req.kilometrageActuel() != null ? req.kilometrageActuel() : 0);
        appliquer(v, req);
        v.setCreatedBy(auteurId);
        return toResponse(vehiculeRepository.save(v), null, LocalDate.now());
    }

    /**
     * Modifie la fiche. Le kilométrage ne peut pas reculer, et ne se corrige pas
     * pendant une sortie (il sera mis à jour au retour).
     */
    public VehiculeResponse modifier(UUID id, VehiculeRequest req) {
        Vehicule v = trouver(id);
        String immat = normaliser(req.immatriculation());
        if (vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot(immat, id)) {
            throw new BusinessException("Un véhicule immatriculé " + immat + " existe déjà");
        }
        UUID enCours = sortieRepository.findFirstByVehiculeIdAndDateRetourIsNull(id).map(SortieVehicule::getId).orElse(null);
        if (req.kilometrageActuel() != null && !req.kilometrageActuel().equals(v.getKilometrageActuel())) {
            if (enCours != null) {
                throw new BusinessException("Le véhicule est en sortie : le kilométrage sera mis à jour au retour");
            }
            if (req.kilometrageActuel() < v.getKilometrageActuel()) {
                throw new BusinessException("Le kilométrage ne peut pas reculer (actuel : " + v.getKilometrageActuel() + " km)");
            }
            v.setKilometrageActuel(req.kilometrageActuel());
        }
        v.setImmatriculation(immat);
        appliquer(v, req);
        return toResponse(vehiculeRepository.save(v), enCours, LocalDate.now());
    }

    private void appliquer(Vehicule v, VehiculeRequest req) {
        verifierProjet(req.projetId());
        verifierPersonne(req.conducteurHabituelId());
        v.setMarqueModele(req.marqueModele().trim());
        v.setAnnee(req.annee());
        v.setKilometrageRevision(req.kilometrageRevision());
        if (req.etat() != null) {
            v.setEtat(req.etat());
        }
        v.setProjetId(req.projetId());
        v.setConducteurHabituelId(req.conducteurHabituelId());
        v.setDateExpirationAssurance(req.dateExpirationAssurance());
        v.setDateVisiteTechnique(req.dateVisiteTechnique());
        v.setTypeCarburant(req.typeCarburant());
        v.setNotes(texte(req.notes()));
    }

    /**
     * Alertes d'un véhicule à une date donnée : assurance et visite technique
     * expirées ou à moins de 30 jours, révision dépassée ou à moins de 500 km.
     * Rien pour un véhicule hors service.
     */
    static List<AlerteVehicule> alertes(Vehicule v, LocalDate aujourdhui) {
        List<AlerteVehicule> alertes = new ArrayList<>();
        if (v.getEtat() == EtatMateriel.HORS_SERVICE) {
            return alertes;
        }
        echeance(alertes, v.getDateExpirationAssurance(), aujourdhui, "ASSURANCE", "Assurance");
        echeance(alertes, v.getDateVisiteTechnique(), aujourdhui, "VISITE", "Visite technique");
        Integer rev = v.getKilometrageRevision();
        int km = v.getKilometrageActuel() != null ? v.getKilometrageActuel() : 0;
        if (rev != null) {
            if (km >= rev) {
                alertes.add(new AlerteVehicule("REVISION_DEPASSEE", "Révision à faire (prévue à " + rev + " km)"));
            } else if (rev - km <= KM_ALERTE_REVISION) {
                alertes.add(new AlerteVehicule("REVISION_PROCHE", "Révision dans " + (rev - km) + " km"));
            }
        }
        return alertes;
    }

    private static void echeance(List<AlerteVehicule> alertes, LocalDate date, LocalDate aujourdhui,
                                 String code, String libelle) {
        if (date == null) {
            return;
        }
        long jours = ChronoUnit.DAYS.between(aujourdhui, date);
        if (jours < 0) {
            alertes.add(new AlerteVehicule(code + "_EXPIREE", libelle + " expirée depuis le " + date.format(JJ_MM_AAAA)));
        } else if (jours <= JOURS_ALERTE) {
            alertes.add(new AlerteVehicule(code + "_PROCHE",
                    libelle + (jours == 0 ? " expire aujourd'hui" : " expire dans " + jours + " jour" + (jours > 1 ? "s" : ""))));
        }
    }

    // ── Carnet de bord ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SortieResponse> sorties(UUID vehiculeId) {
        trouver(vehiculeId);
        List<SortieVehicule> liste = sortieRepository.findByVehiculeIdOrderByDateDepartDesc(vehiculeId);
        precharger(userRepository, liste, SortieVehicule::getConducteurId);
        precharger(projetRepository, liste, SortieVehicule::getProjetId);
        return liste.stream().map(this::toResponse).toList();
    }

    /** Départ d'un véhicule : refusé s'il est déjà sorti, en réparation ou hors service. */
    public SortieResponse depart(UUID vehiculeId, DepartRequest req, UUID auteurId) {
        Vehicule v = trouver(vehiculeId);
        if (v.getEtat() == EtatMateriel.HORS_SERVICE || v.getEtat() == EtatMateriel.EN_REPARATION) {
            throw new BusinessException("Ce véhicule est " + (v.getEtat() == EtatMateriel.HORS_SERVICE ? "hors service" : "en réparation"));
        }
        sortieRepository.findFirstByVehiculeIdAndDateRetourIsNull(vehiculeId).ifPresent(s -> {
            throw new BusinessException("Ce véhicule est déjà en sortie : enregistrez d'abord son retour");
        });
        verifierPersonne(req.conducteurId());
        verifierProjet(req.projetId());
        int kmVehicule = v.getKilometrageActuel() != null ? v.getKilometrageActuel() : 0;
        int kmDepart = req.kmDepart() != null ? req.kmDepart() : kmVehicule;
        if (kmDepart < kmVehicule) {
            throw new BusinessException("Le kilométrage de départ ne peut pas être inférieur à celui du véhicule (" + kmVehicule + " km)");
        }
        LocalDateTime dateDepart = req.dateDepart() != null ? req.dateDepart() : LocalDateTime.now();
        if (dateDepart.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessException("La date de départ ne peut pas être dans le futur");
        }

        SortieVehicule s = new SortieVehicule();
        s.setVehiculeId(vehiculeId);
        s.setConducteurId(req.conducteurId());
        s.setProjetId(req.projetId());
        s.setDestination(req.destination().trim());
        s.setMotif(texte(req.motif()));
        s.setKmDepart(kmDepart);
        s.setDateDepart(dateDepart);
        s.setCreatedBy(auteurId);
        if (kmDepart > kmVehicule) {
            v.setKilometrageActuel(kmDepart);
            vehiculeRepository.save(v);
        }
        return toResponse(sortieRepository.save(s));
    }

    /** Retour : le kilométrage du véhicule devient celui du retour. */
    public SortieResponse retour(UUID sortieId, RetourRequest req) {
        SortieVehicule s = sortieRepository.findById(sortieId)
                .orElseThrow(() -> new ResourceNotFoundException("Sortie introuvable : " + sortieId));
        if (s.getDateRetour() != null) {
            throw new BusinessException("Le retour de cette sortie est déjà enregistré");
        }
        if (req.kmRetour() < s.getKmDepart()) {
            throw new BusinessException("Le kilométrage au retour ne peut pas être inférieur à celui du départ (" + s.getKmDepart() + " km)");
        }
        LocalDateTime dateRetour = req.dateRetour() != null ? req.dateRetour() : LocalDateTime.now();
        if (dateRetour.isBefore(s.getDateDepart())) {
            throw new BusinessException("Le retour ne peut pas précéder le départ");
        }
        s.setKmRetour(req.kmRetour());
        s.setDateRetour(dateRetour);
        s.setEtatRetour(texte(req.etatRetour()));

        Vehicule v = trouver(s.getVehiculeId());
        if (v.getKilometrageActuel() == null || req.kmRetour() > v.getKilometrageActuel()) {
            v.setKilometrageActuel(req.kmRetour());
            vehiculeRepository.save(v);
        }
        return toResponse(sortieRepository.save(s));
    }

    // ── Personnes (responsables, conducteurs) ───────────────────────────────

    @Transactional(readOnly = true)
    public List<PersonneResponse> personnes() {
        return userRepository.findAll().stream()
                .filter(u -> u.getStatut() == StatutUser.ACTIF)
                .map(u -> new PersonneResponse(u.getId(), u.getPrenom() + " " + u.getNom()))
                .sorted((a, b) -> a.nom().compareToIgnoreCase(b.nom()))
                .toList();
    }

    // ── Outils ──────────────────────────────────────────────────────────────

    private Vehicule trouver(UUID id) {
        return vehiculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule introuvable : " + id));
    }

    private void verifierProjet(UUID projetId) {
        if (projetId != null && !projetRepository.existsById(projetId)) {
            throw new ResourceNotFoundException("Projet introuvable : " + projetId);
        }
    }

    private void verifierPersonne(UUID userId) {
        if (userId != null && !userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Personne introuvable : " + userId);
        }
    }

    /** Immatriculation en majuscules, sans espaces en trop (« dk 1234 ab » → « DK 1234 AB »). */
    static String normaliser(String immatriculation) {
        return immatriculation.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private static String texte(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String nom(UUID userId) {
        return userId == null ? null
                : userRepository.findById(userId).map(u -> u.getPrenom() + " " + u.getNom()).orElse(null);
    }

    private String projetCode(UUID projetId) {
        return projetId == null ? null : projetRepository.findById(projetId).map(p -> p.getCode()).orElse(null);
    }

    private VehiculeResponse toResponse(Vehicule v, UUID sortieEnCours, LocalDate aujourdhui) {
        return new VehiculeResponse(v.getId(), v.getImmatriculation(), v.getMarqueModele(), v.getAnnee(),
                v.getKilometrageActuel(), v.getKilometrageRevision(), v.getEtat(), v.getProjetId(),
                projetCode(v.getProjetId()), v.getConducteurHabituelId(), nom(v.getConducteurHabituelId()),
                v.getDateExpirationAssurance(), v.getDateVisiteTechnique(), v.getTypeCarburant(), v.getNotes(),
                alertes(v, aujourdhui), sortieEnCours, v.getCreatedAt());
    }

    private SortieResponse toResponse(SortieVehicule s) {
        Integer distance = s.getKmRetour() != null ? s.getKmRetour() - s.getKmDepart() : null;
        return new SortieResponse(s.getId(), s.getVehiculeId(), s.getConducteurId(), nom(s.getConducteurId()),
                s.getProjetId(), projetCode(s.getProjetId()), s.getDestination(), s.getMotif(), s.getKmDepart(),
                s.getKmRetour(), distance, s.getDateDepart(), s.getDateRetour(), s.getEtatRetour(),
                nom(s.getCreatedBy()));
    }
}
