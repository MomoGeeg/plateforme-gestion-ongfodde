package ong.fodde.modules.missions.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.dto.AvanceRequest;
import ong.fodde.modules.financier.service.AvanceService;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.historique.entity.TypeDemande;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.logistique.entity.EtatMateriel;
import ong.fodde.modules.logistique.entity.Vehicule;
import ong.fodde.modules.logistique.repository.VehiculeRepository;
import ong.fodde.modules.missions.dto.MissionDtos.Disponibilites;
import ong.fodde.modules.missions.dto.MissionDtos.FraisRequest;
import ong.fodde.modules.missions.dto.MissionDtos.FraisResponse;
import ong.fodde.modules.missions.dto.MissionDtos.MissionRequest;
import ong.fodde.modules.missions.dto.MissionDtos.MissionResponse;
import ong.fodde.modules.missions.dto.MissionDtos.OrganisationRequest;
import ong.fodde.modules.missions.dto.MissionDtos.ParticipantResponse;
import ong.fodde.modules.missions.dto.MissionDtos.PersonneDisponible;
import ong.fodde.modules.missions.dto.MissionDtos.PreparationRequest;
import ong.fodde.modules.missions.dto.MissionDtos.VehiculeDisponible;
import ong.fodde.modules.missions.entity.FraisMission;
import ong.fodde.modules.missions.entity.Mission;
import ong.fodde.modules.missions.entity.StatutMission;
import ong.fodde.modules.missions.repository.MissionRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.rh.entity.DemandeConge;
import ong.fodde.modules.rh.entity.Employe;
import ong.fodde.modules.rh.entity.StatutConge;
import ong.fodde.modules.rh.entity.StatutEmploye;
import ong.fodde.modules.rh.repository.DemandeCongeRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.exception.ResourceNotFoundException;
import ong.fodde.shared.securite.AccesPersonnel;

import static ong.fodde.modules.missions.entity.StatutMission.*;

/**
 * Missions, d'après l'organisation de l'ONG :
 * <ol>
 *   <li>le demandeur saisit l'ordre de mission (destination, dates, objet, équipe proposée, besoins) ;</li>
 *   <li>son supérieur direct donne son accord (sauté s'il n'en a pas, ou si c'est le SE, qui
 *       intervient plus loin si nécessaire) ;</li>
 *   <li>la logistique affecte véhicule et chauffeur libres (sauté sans besoin logistique) ;</li>
 *   <li>le RAF arrête l'équipe et le budget, puis valide ;</li>
 *   <li>le SE valide au-delà du seuil, ou si le RAF le demande ;</li>
 *   <li>la comptabilité débloque les fonds : une avance au chef de mission (sauté sans frais) ;</li>
 *   <li>la mission est prête : ordre de mission imprimable et envoyé à l'équipe.</li>
 * </ol>
 */
@Service
@Transactional
public class MissionService {

    /** Rôles qui voient toutes les missions (traitement ou supervision). */
    static final String[] ROLES_GESTION = {
        "ROLE_RAF", "ROLE_SE", "ROLE_ADMIN", "ROLE_RESPONSABLE_PROGRAMMES",
        "ROLE_RESPONSABLE_LOGISTIQUE", "ROLE_COMPTABLE"
    };

    /** Une mission rejetée ou annulée ne retient plus personne ni aucun véhicule. */
    static final Set<StatutMission> INACTIVES = Set.of(REJETEE, ANNULEE);

    private static final Set<StatutConge> CONGES_RETENUS = Set.of(StatutConge.VALIDE_N1, StatutConge.APPROUVE);
    private static final DateTimeFormatter JJ_MM_AAAA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MissionRepository missionRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final EmployeRepository employeRepository;
    private final VehiculeRepository vehiculeRepository;
    private final DemandeCongeRepository congeRepository;
    private final AvanceService avanceService;
    private final HistoriqueService historiqueService;
    private final AccesPersonnel accesPersonnel;
    private final BigDecimal seuilSe;

    public MissionService(MissionRepository missionRepository,
                          UserRepository userRepository,
                          ProjetRepository projetRepository,
                          EmployeRepository employeRepository,
                          VehiculeRepository vehiculeRepository,
                          DemandeCongeRepository congeRepository,
                          AvanceService avanceService,
                          HistoriqueService historiqueService,
                          AccesPersonnel accesPersonnel,
                          @Value("${app.missions.seuil-se:500000}") BigDecimal seuilSe) {
        this.missionRepository = missionRepository;
        this.userRepository = userRepository;
        this.projetRepository = projetRepository;
        this.employeRepository = employeRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.congeRepository = congeRepository;
        this.avanceService = avanceService;
        this.historiqueService = historiqueService;
        this.accesPersonnel = accesPersonnel;
        this.seuilSe = seuilSe;
    }

    // ═════════════════════════════════════════════════════════════════════
    // Lecture
    // ═════════════════════════════════════════════════════════════════════

    /** Toutes les missions pour les rôles de gestion ; sinon celles qui concernent l'utilisateur. */
    @Transactional(readOnly = true)
    public List<MissionResponse> lister(User user) {
        List<Mission> missions = missionRepository.findAll();
        if (!accesPersonnel.aUnRole(user, ROLES_GESTION)) {
            UUID fiche = ficheDe(user.getId());
            missions = missions.stream().filter(m -> concerne(m, user.getId(), fiche)).toList();
        }
        return missions.stream()
                .sorted(Comparator.comparing(Mission::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    /** Missions demandées par l'utilisateur ou dont il fait partie (accueil « Mes demandes »). */
    @Transactional(readOnly = true)
    public List<MissionResponse> mesMissions(User user) {
        UUID fiche = ficheDe(user.getId());
        return missionRepository.findAll().stream()
                .filter(m -> user.getId().equals(m.getEmployeId())
                        || (fiche != null && (m.getParticipantIds().contains(fiche) || fiche.equals(m.getChauffeurId()))))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MissionResponse obtenir(UUID id, User user) {
        Mission m = getOrThrow(id);
        verifierVisible(m, user);
        return toResponse(m);
    }

    @Transactional(readOnly = true)
    public List<EtapeHistorique> historique(UUID id, User user) {
        Mission m = getOrThrow(id);
        verifierVisible(m, user);
        List<EtapeHistorique> anciennes = m.getStatut() == REJETEE
                ? List.of(historiqueService.reconstituer(null, REJETEE, null, m.getMotifRejet(), null))
                : m.getStatut() == SOUMISE ? List.of()
                : List.of(historiqueService.reconstituer(null, m.getStatut(), null, null, null));
        EtapeHistorique creation = historiqueService.reconstituer(null, SOUMISE, m.getEmployeId(), null,
                m.getCreatedAt());
        return historiqueService.historique(TypeDemande.MISSION, id, creation, anciennes);
    }

    /**
     * Agents, chauffeurs et véhicules libres sur la période : pas déjà sur une autre mission
     * (hors rejetées et annulées), pas en congé ; véhicules ni en réparation ni hors service.
     */
    @Transactional(readOnly = true)
    public Disponibilites disponibilites(LocalDate debut, LocalDate fin, UUID missionId) {
        verifierDates(debut, fin);
        List<Mission> autres = missionRepository.chevauchant(debut, fin, INACTIVES).stream()
                .filter(m -> !m.getId().equals(missionId))
                .toList();
        Set<UUID> personnesPrises = new HashSet<>();
        Set<UUID> vehiculesPris = new HashSet<>();
        for (Mission m : autres) {
            personnesPrises.addAll(m.getParticipantIds());
            if (m.getChauffeurId() != null) {
                personnesPrises.add(m.getChauffeurId());
            }
            if (m.getVehiculeId() != null) {
                vehiculesPris.add(m.getVehiculeId());
            }
        }
        congeRepository.findAll().stream()
                .filter(c -> CONGES_RETENUS.contains(c.getStatut()) && chevauche(c, debut, fin))
                .map(DemandeConge::getEmployeId)
                .forEach(personnesPrises::add);

        List<Employe> presents = employeRepository.findAll().stream()
                .filter(e -> e.getStatut() != StatutEmploye.PARTI && !Boolean.TRUE.equals(e.getIsDeleted()))
                .filter(e -> !personnesPrises.contains(e.getId()))
                .sorted(Comparator.comparing((Employe e) -> e.getNom().toLowerCase())
                        .thenComparing(e -> e.getPrenom().toLowerCase()))
                .toList();
        List<PersonneDisponible> agents = presents.stream().filter(e -> !estChauffeur(e)).map(this::personne).toList();
        List<PersonneDisponible> chauffeurs = presents.stream().filter(MissionService::estChauffeur)
                .map(this::personne).toList();
        List<VehiculeDisponible> vehicules = vehiculeRepository.findAllByOrderByImmatriculationAsc().stream()
                .filter(v -> v.getEtat() != EtatMateriel.EN_REPARATION && v.getEtat() != EtatMateriel.HORS_SERVICE)
                .filter(v -> !vehiculesPris.contains(v.getId()))
                .map(v -> new VehiculeDisponible(v.getId(), libelle(v)))
                .toList();
        return new Disponibilites(agents, chauffeurs, vehicules);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Circuit
    // ═════════════════════════════════════════════════════════════════════

    /** 1. Le demandeur saisit l'ordre de mission. */
    public MissionResponse creer(MissionRequest req, User demandeur) {
        verifierDates(req.dateDebut(), req.dateFin());
        if (!projetRepository.existsById(req.projetId())) {
            throw new ResourceNotFoundException("Projet introuvable : " + req.projetId());
        }
        Mission m = new Mission();
        m.setEmployeId(demandeur.getId());
        m.setCreatedBy(demandeur.getId());
        m.setProjetId(req.projetId());
        m.setDestination(req.destination().trim());
        m.setDateDebut(req.dateDebut());
        m.setDateFin(req.dateFin());
        m.setMotif(req.motif().trim());
        m.setBesoinVehicule(req.besoinVehicule());
        m.setBesoinsMateriel(vide(req.besoinsMateriel()) ? null : req.besoinsMateriel().trim());
        definirEquipe(m, req.participantIds(), req.chefMissionId(), null);
        m.setBudgetEstime(BigDecimal.ZERO);

        // Supérieur direct : son accord est la première étape, sauf s'il n'y en a pas ou si c'est le SE.
        m.setN1Id(superieurQuiApprouve(demandeur.getId()).orElse(null));
        m.setStatut(m.getN1Id() != null ? SOUMISE : apresAccordN1(m));

        Mission saved = missionRepository.save(m);
        historiqueService.enregistrer(TypeDemande.MISSION, saved.getId(), null, saved.getStatut(),
                demandeur.getId(), null);
        return toResponse(saved);
    }

    /** 2. Accord du supérieur direct. */
    public MissionResponse approuverN1(UUID id, User user) {
        Mission m = getOrThrow(id);
        exigerStatut(m, SOUMISE);
        if (!user.getId().equals(m.getN1Id()) && !accesPersonnel.aUnRole(user, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Seul le supérieur direct du demandeur donne cet accord");
        }
        return changer(m, apresAccordN1(m), user, null);
    }

    /** 3. La logistique affecte véhicule et chauffeur, libres sur la période. */
    public MissionResponse organiser(UUID id, OrganisationRequest req, User user) {
        Mission m = getOrThrow(id);
        exigerStatut(m, APPROUVEE_N1);
        if (m.isBesoinVehicule() && (req.vehiculeId() == null || req.chauffeurId() == null)) {
            throw new BusinessException("Choisissez le véhicule et le chauffeur");
        }
        Disponibilites libres = disponibilites(m.getDateDebut(), m.getDateFin(), m.getId());
        if (req.vehiculeId() != null && libres.vehicules().stream().noneMatch(v -> v.id().equals(req.vehiculeId()))) {
            throw new BusinessException("Ce véhicule n'est pas libre sur ces dates (autre mission, réparation ou hors service)");
        }
        if (req.chauffeurId() != null) {
            if (libres.chauffeurs().stream().noneMatch(c -> c.id().equals(req.chauffeurId()))) {
                throw new BusinessException("Ce chauffeur n'est pas libre sur ces dates (autre mission ou congé)");
            }
            if (m.getParticipantIds().contains(req.chauffeurId())) {
                throw new BusinessException("Le chauffeur fait déjà partie de l'équipe");
            }
        }
        m.setVehiculeId(req.vehiculeId());
        m.setChauffeurId(req.chauffeurId());
        m.setNotesLogistique(vide(req.notesLogistique()) ? null : req.notesLogistique().trim());
        return changer(m, ORGANISEE, user, null);
    }

    /** 4a. Le RAF arrête l'équipe et le budget (modifiable tant qu'il n'a pas validé). */
    public MissionResponse preparer(UUID id, PreparationRequest req, User user) {
        Mission m = getOrThrow(id);
        exigerStatut(m, ORGANISEE);
        definirEquipe(m, req.participantIds(), req.chefMissionId(), m.getId());
        List<FraisMission> frais = new ArrayList<>();
        for (FraisRequest f : Optional.ofNullable(req.frais()).orElse(List.of())) {
            if (f.categorie() == null) {
                throw new BusinessException("Choisissez la catégorie de chaque ligne du budget");
            }
            if (f.quantite() == null || f.quantite().signum() <= 0
                    || f.prixUnitaire() == null || f.prixUnitaire().signum() < 0) {
                throw new BusinessException("Chaque ligne du budget demande une quantité positive et un prix");
            }
            frais.add(new FraisMission(f.categorie(), vide(f.libelle()) ? null : f.libelle().trim(),
                    f.quantite(), f.prixUnitaire()));
        }
        m.getFrais().clear();
        m.getFrais().addAll(frais);
        m.setBudgetEstime(total(m));
        m.setSoumettreSe(req.soumettreSe());
        return toResponse(missionRepository.save(m));
    }

    /** 4b. Validation du RAF : vers le SE si nécessaire, sinon vers la comptabilité. */
    public MissionResponse validerRaf(UUID id, User user) {
        Mission m = getOrThrow(id);
        exigerStatut(m, ORGANISEE);
        if (m.getParticipantIds().isEmpty()) {
            throw new BusinessException("Ajoutez au moins une personne à l'équipe avant de valider");
        }
        return changer(m, validationSeRequise(m) ? EN_VALIDATION_SE : apresValidation(m), user, null);
    }

    /** 5. Validation du Secrétaire exécutif. */
    public MissionResponse validerSe(UUID id, User user) {
        Mission m = getOrThrow(id);
        exigerStatut(m, EN_VALIDATION_SE);
        return changer(m, apresValidation(m), user, null);
    }

    /** 6. La comptabilité débloque les fonds : une avance à justifier au retour. */
    public MissionResponse debloquerFonds(UUID id, User user) {
        Mission m = getOrThrow(id);
        exigerStatut(m, VALIDEE);
        UUID beneficiaire = Optional.ofNullable(m.getChefMissionId())
                .flatMap(employeRepository::findById)
                .map(Employe::getUserId)
                .orElse(m.getEmployeId());
        String motif = "Mission " + m.getDestination() + " du " + m.getDateDebut().format(JJ_MM_AAAA)
                + " au " + m.getDateFin().format(JJ_MM_AAAA);
        UUID avance = avanceService.creerAvance(new AvanceRequest(beneficiaire, m.getProjetId(), m.getBudgetEstime(),
                motif, LocalDate.now())).id();
        m.setAvanceId(avance);
        return changer(m, PRETE, user, null);
    }

    /** Rejet, motif obligatoire, par la personne qui a la main à cette étape. */
    public MissionResponse rejeter(UUID id, String motif, User user) {
        Mission m = getOrThrow(id);
        if (vide(motif)) {
            throw new BusinessException("Le motif de rejet est obligatoire");
        }
        boolean admin = accesPersonnel.aUnRole(user, "ROLE_ADMIN");
        boolean autorise = switch (m.getStatut()) {
            case SOUMISE -> user.getId().equals(m.getN1Id());
            case APPROUVEE_N1, ORGANISEE -> accesPersonnel.aUnRole(user, "ROLE_RAF");
            case EN_VALIDATION_SE -> accesPersonnel.aUnRole(user, "ROLE_SE");
            default -> throw new BusinessException("Cette mission ne peut plus être rejetée");
        };
        if (!autorise && !admin) {
            throw new AccessDeniedException("Ce n'est pas à vous de décider à cette étape");
        }
        m.setMotifRejet(motif.trim());
        return changer(m, REJETEE, user, motif.trim());
    }

    /** Annulation par le demandeur ou le RAF, tant que les fonds ne sont pas débloqués. */
    public MissionResponse annuler(UUID id, String motif, User user) {
        Mission m = getOrThrow(id);
        if (Set.of(PRETE, REJETEE, ANNULEE, VALIDEE_RAF).contains(m.getStatut())) {
            throw new BusinessException("Cette mission ne peut plus être annulée");
        }
        if (!user.getId().equals(m.getEmployeId()) && !accesPersonnel.aUnRole(user, "ROLE_RAF", "ROLE_ADMIN")) {
            throw new AccessDeniedException("Seuls le demandeur et le RAF peuvent annuler la mission");
        }
        m.setMotifRejet(vide(motif) ? null : motif.trim());
        return changer(m, ANNULEE, user, m.getMotifRejet());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Règles
    // ═════════════════════════════════════════════════════════════════════

    static StatutMission apresAccordN1(Mission m) {
        return m.isBesoinVehicule() || !vide(m.getBesoinsMateriel()) ? APPROUVEE_N1 : ORGANISEE;
    }

    static StatutMission apresValidation(Mission m) {
        return total(m).signum() > 0 ? VALIDEE : PRETE;
    }

    boolean validationSeRequise(Mission m) {
        return m.isSoumettreSe() || total(m).compareTo(seuilSe) >= 0;
    }

    static BigDecimal total(Mission m) {
        return m.getFrais().stream().map(FraisMission::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Compte du supérieur direct qui doit donner son accord, s'il y a lieu. */
    Optional<UUID> superieurQuiApprouve(UUID demandeurId) {
        return employeRepository.findByUserId(demandeurId)
                .map(Employe::getSuperieurId)
                .flatMap(employeRepository::findById)
                .filter(sup -> sup.getStatut() != StatutEmploye.PARTI)
                .map(Employe::getUserId)
                .filter(compte -> !compte.equals(demandeurId))
                .filter(compte -> userRepository.findById(compte)
                        .map(u -> !accesPersonnel.aUnRole(u, "ROLE_SE"))
                        .orElse(false));
    }

    /** Équipe : fiches présentes, libres sur la période ; le chef en fait partie (le premier par défaut). */
    private void definirEquipe(Mission m, List<UUID> ids, UUID chefId, UUID missionId) {
        Set<UUID> equipe = new LinkedHashSet<>(Optional.ofNullable(ids).orElse(List.of()));
        if (!equipe.isEmpty()) {
            Set<UUID> libres = new HashSet<>();
            Disponibilites d = disponibilites(m.getDateDebut(), m.getDateFin(), missionId);
            d.agents().forEach(a -> libres.add(a.id()));
            d.chauffeurs().forEach(c -> libres.add(c.id()));
            for (UUID e : equipe) {
                Employe fiche = employeRepository.findById(e)
                        .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable : " + e));
                if (!libres.contains(e)) {
                    throw new BusinessException(fiche.getPrenom() + " " + fiche.getNom()
                            + " n'est pas libre sur ces dates (autre mission, congé ou départ)");
                }
            }
            if (m.getChauffeurId() != null && equipe.contains(m.getChauffeurId())) {
                throw new BusinessException("Le chauffeur de la mission ne peut pas être aussi dans l'équipe");
            }
        }
        if (chefId != null && !equipe.contains(chefId)) {
            throw new BusinessException("Le chef de mission doit faire partie de l'équipe");
        }
        m.getParticipantIds().clear();
        m.getParticipantIds().addAll(equipe);
        m.setChefMissionId(chefId != null ? chefId : equipe.stream().findFirst().orElse(null));
    }

    private MissionResponse changer(Mission m, StatutMission apres, User user, String motif) {
        StatutMission avant = m.getStatut();
        m.setStatut(apres);
        Mission saved = missionRepository.save(m);
        historiqueService.enregistrer(TypeDemande.MISSION, saved.getId(), avant, apres, user.getId(), motif);
        return toResponse(saved);
    }

    private static void exigerStatut(Mission m, StatutMission attendu) {
        if (m.getStatut() != attendu) {
            throw new BusinessException("Action impossible à cette étape de la mission");
        }
    }

    private static void verifierDates(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null) {
            throw new BusinessException("Indiquez les dates de départ et de retour");
        }
        if (fin.isBefore(debut)) {
            throw new BusinessException("La date de retour doit suivre la date de départ");
        }
    }

    private boolean concerne(Mission m, UUID userId, UUID fiche) {
        return userId.equals(m.getEmployeId()) || userId.equals(m.getN1Id())
                || (fiche != null && (m.getParticipantIds().contains(fiche) || fiche.equals(m.getChauffeurId())));
    }

    private void verifierVisible(Mission m, User user) {
        if (!accesPersonnel.aUnRole(user, ROLES_GESTION) && !concerne(m, user.getId(), ficheDe(user.getId()))) {
            throw new AccessDeniedException("Cette mission ne vous concerne pas");
        }
    }

    private UUID ficheDe(UUID userId) {
        return employeRepository.findByUserId(userId).map(Employe::getId).orElse(null);
    }

    private static boolean chevauche(DemandeConge c, LocalDate debut, LocalDate fin) {
        return c.getDateDebut() != null && c.getDateFin() != null
                && !c.getDateDebut().isAfter(fin) && !c.getDateFin().isBefore(debut);
    }

    /** « Chauffeur » dans l'intitulé du poste (accents et majuscules ignorés). */
    static boolean estChauffeur(Employe e) {
        String poste = Normalizer.normalize(Optional.ofNullable(e.getPoste()).orElse(""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
        return poste.contains("chauffeur");
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static String libelle(Vehicule v) {
        return v.getImmatriculation() + (v.getMarqueModele() != null ? " — " + v.getMarqueModele() : "");
    }

    private PersonneDisponible personne(Employe e) {
        return new PersonneDisponible(e.getId(), e.getPrenom() + " " + e.getNom(), e.getPoste(), e.getTelephone());
    }

    private Mission getOrThrow(UUID id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : " + id));
    }

    // ═════════════════════════════════════════════════════════════════════
    // Conversion
    // ═════════════════════════════════════════════════════════════════════

    private String nomCompte(UUID userId) {
        return userId == null ? null
                : userRepository.findById(userId).map(u -> u.getPrenom() + " " + u.getNom()).orElse(null);
    }

    private MissionResponse toResponse(Mission m) {
        List<ParticipantResponse> participants = m.getParticipantIds().stream()
                .map(employeRepository::findById)
                .flatMap(Optional::stream)
                .map(e -> new ParticipantResponse(e.getId(), e.getPrenom() + " " + e.getNom(), e.getPoste(),
                        e.getTelephone(), e.getId().equals(m.getChefMissionId())))
                .toList();
        Optional<Employe> chauffeur = Optional.ofNullable(m.getChauffeurId()).flatMap(employeRepository::findById);
        String vehicule = Optional.ofNullable(m.getVehiculeId()).flatMap(vehiculeRepository::findById)
                .map(MissionService::libelle).orElse(null);
        List<FraisResponse> frais = m.getFrais().stream()
                .map(f -> new FraisResponse(f.getCategorie(), f.getLibelle(), f.getQuantite(), f.getPrixUnitaire(),
                        f.montant()))
                .toList();
        return new MissionResponse(
                m.getId(),
                m.getEmployeId(),
                nomCompte(m.getEmployeId()),
                m.getN1Id(),
                nomCompte(m.getN1Id()),
                m.getProjetId(),
                projetRepository.findById(m.getProjetId()).map(p -> p.getCode()).orElse(null),
                m.getDestination(),
                m.getDateDebut(),
                m.getDateFin(),
                ChronoUnit.DAYS.between(m.getDateDebut(), m.getDateFin()) + 1,
                m.getMotif(),
                m.isBesoinVehicule(),
                m.getBesoinsMateriel(),
                participants,
                m.getVehiculeId(),
                vehicule,
                m.getChauffeurId(),
                chauffeur.map(e -> e.getPrenom() + " " + e.getNom()).orElse(null),
                chauffeur.map(Employe::getTelephone).orElse(null),
                m.getNotesLogistique(),
                frais,
                m.getBudgetEstime(),
                m.isSoumettreSe(),
                validationSeRequise(m),
                seuilSe,
                m.getStatut(),
                m.getMotifRejet(),
                m.getAvanceId(),
                m.getCreatedAt());
    }
}
