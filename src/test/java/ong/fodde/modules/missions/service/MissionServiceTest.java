package ong.fodde.modules.missions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.financier.dto.AvanceRequest;
import ong.fodde.modules.financier.dto.AvanceResponse;
import ong.fodde.modules.financier.service.AvanceService;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.logistique.entity.EtatMateriel;
import ong.fodde.modules.logistique.entity.Vehicule;
import ong.fodde.modules.logistique.repository.VehiculeRepository;
import ong.fodde.modules.missions.dto.MissionDtos.FraisRequest;
import ong.fodde.modules.missions.dto.MissionDtos.MissionRequest;
import ong.fodde.modules.missions.dto.MissionDtos.MissionResponse;
import ong.fodde.modules.missions.dto.MissionDtos.OrganisationRequest;
import ong.fodde.modules.missions.dto.MissionDtos.PreparationRequest;
import ong.fodde.modules.missions.entity.CategorieFrais;
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
import ong.fodde.shared.securite.AccesPersonnel;

/** Circuit des missions : étapes sautées, droits de chaque étape, disponibilités, budget, fonds. */
class MissionServiceTest {

    private final MissionRepository missions = mock(MissionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final EmployeRepository employes = mock(EmployeRepository.class);
    private final VehiculeRepository vehicules = mock(VehiculeRepository.class);
    private final DemandeCongeRepository conges = mock(DemandeCongeRepository.class);
    private final AvanceService avances = mock(AvanceService.class);
    private final MissionService service = new MissionService(missions, users, projets, employes, vehicules, conges,
            avances, mock(HistoriqueService.class), new AccesPersonnel(employes), new BigDecimal("500000"));

    private final List<Employe> toutesFiches = new ArrayList<>();
    private final List<Mission> autresMissions = new ArrayList<>();
    private final List<Vehicule> parc = new ArrayList<>();
    private final List<DemandeConge> tousConges = new ArrayList<>();

    private final LocalDate debut = LocalDate.of(2026, 12, 1);
    private final LocalDate fin = LocalDate.of(2026, 12, 4);
    private final UUID projet = UUID.randomUUID();

    @BeforeEach
    void preparer() {
        when(projets.existsById(projet)).thenReturn(true);
        when(missions.save(any())).thenAnswer(i -> {
            Mission m = i.getArgument(0);
            if (m.getId() == null) {
                m.setId(UUID.randomUUID());
            }
            when(missions.findById(m.getId())).thenReturn(Optional.of(m));
            return m;
        });
        when(missions.chevauchant(any(), any(), any())).thenAnswer(i -> autresMissions);
        when(missions.findAll()).thenAnswer(i -> autresMissions);
        when(employes.findAll()).thenAnswer(i -> toutesFiches);
        when(vehicules.findAllByOrderByImmatriculationAsc()).thenAnswer(i -> parc);
        when(conges.findAll()).thenAnswer(i -> tousConges);
    }

    // ── Jeu d'essai ───────────────────────────────────────

    private User compte(String... roles) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom("Compte");
        u.setPrenom("Test");
        Set<Role> set = new HashSet<>();
        for (String nom : roles) {
            Role r = new Role();
            r.setNom(nom);
            set.add(r);
        }
        u.setRoles(set);
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    private Employe fiche(String poste, User compte, Employe superieur) {
        Employe e = new Employe();
        e.setId(UUID.randomUUID());
        e.setNom("Ba");
        e.setPrenom(poste);
        e.setPoste(poste);
        e.setStatut(StatutEmploye.ACTIF);
        if (compte != null) {
            e.setUserId(compte.getId());
            when(employes.findByUserId(compte.getId())).thenReturn(Optional.of(e));
        }
        if (superieur != null) {
            e.setSuperieurId(superieur.getId());
        }
        when(employes.findById(e.getId())).thenReturn(Optional.of(e));
        toutesFiches.add(e);
        return e;
    }

    private Vehicule vehicule(EtatMateriel etat) {
        Vehicule v = new Vehicule();
        v.setId(UUID.randomUUID());
        v.setImmatriculation("DK-" + parc.size());
        v.setEtat(etat);
        when(vehicules.findById(v.getId())).thenReturn(Optional.of(v));
        parc.add(v);
        return v;
    }

    private MissionRequest demande(boolean vehicule, Employe... equipe) {
        return new MissionRequest(projet, "Vélingara", debut, fin, "Formation des relais",
                java.util.Arrays.stream(equipe).map(Employe::getId).toList(), null, vehicule, null);
    }

    /** Mission arrivée chez le RAF (sans supérieur, sans besoin logistique). */
    private MissionResponse chezLeRaf(User demandeur, Employe agent) {
        MissionResponse m = service.creer(demande(false, agent), demandeur);
        assertEquals(StatutMission.ORGANISEE, m.statut());
        return m;
    }

    private PreparationRequest budget(Employe agent, String prixUnitaire, boolean soumettreSe) {
        return new PreparationRequest(List.of(agent.getId()), agent.getId(),
                List.of(new FraisRequest(CategorieFrais.PER_DIEM, "Per diem", new BigDecimal("4"),
                        new BigDecimal(prixUnitaire))), soumettreSe);
    }

    // ── Création : étapes sautées ─────────────────────────

    @Test
    void unChargeDeProgrammeAttendLAccordDuRp() {
        User rp = compte("ROLE_RESPONSABLE_PROGRAMMES");
        Employe ficheRp = fiche("Responsable des programmes", rp, null);
        User cp = compte("ROLE_CHARGE_PROGRAMME");
        Employe ficheCp = fiche("Chargé de programme", cp, ficheRp);
        MissionResponse m = service.creer(demande(true, ficheCp), cp);
        assertEquals(StatutMission.SOUMISE, m.statut());
        assertEquals(rp.getId(), m.n1Id());
    }

    @Test
    void sousLeSeLAccordN1EstSauteEtLaLogistiqueSuit() {
        User se = compte("ROLE_SE");
        Employe ficheSe = fiche("Secrétaire exécutif", se, null);
        User rp = compte("ROLE_RESPONSABLE_PROGRAMMES");
        Employe ficheRp = fiche("Responsable des programmes", rp, ficheSe);
        MissionResponse m = service.creer(demande(true, ficheRp), rp);
        assertEquals(StatutMission.APPROUVEE_N1, m.statut());
        assertEquals(null, m.n1Id());
    }

    @Test
    void sansBesoinLogistiqueLaMissionVaDirectementAuRaf() {
        User raf = compte("ROLE_RAF");
        Employe ficheRaf = fiche("RAF", raf, null);
        chezLeRaf(raf, ficheRaf);
    }

    @Test
    void lesDatesIncoherentesSontRefusees() {
        User u = compte();
        MissionRequest r = new MissionRequest(projet, "Kolda", fin, debut, "Objet", List.of(), null, false, null);
        assertThrows(BusinessException.class, () -> service.creer(r, u));
    }

    @Test
    void unAgentEnCongeNePeutPasPartir() {
        User u = compte("ROLE_RAF");
        Employe agent = fiche("Animateur", null, null);
        DemandeConge c = new DemandeConge();
        c.setEmployeId(agent.getId());
        c.setDateDebut(debut.plusDays(2));
        c.setDateFin(fin.plusDays(5));
        c.setStatut(StatutConge.APPROUVE);
        tousConges.add(c);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.creer(demande(false, agent), u));
        assertTrue(ex.getMessage().contains("pas libre"));
    }

    // ── Accord du supérieur ───────────────────────────────

    @Test
    void seulLeSuperieurDirectDonneSonAccord() {
        User rp = compte("ROLE_RESPONSABLE_PROGRAMMES");
        Employe ficheRp = fiche("Responsable des programmes", rp, null);
        User cp = compte("ROLE_CHARGE_PROGRAMME");
        Employe ficheCp = fiche("Chargé de programme", cp, ficheRp);
        MissionResponse m = service.creer(demande(false, ficheCp), cp);
        User autre = compte("ROLE_RAF");
        assertThrows(AccessDeniedException.class, () -> service.approuverN1(m.id(), autre));
        assertEquals(StatutMission.ORGANISEE, service.approuverN1(m.id(), rp).statut());
    }

    @Test
    void leRafNeRejettePasALaPlaceDuSuperieur() {
        User rp = compte("ROLE_RESPONSABLE_PROGRAMMES");
        Employe ficheRp = fiche("Responsable des programmes", rp, null);
        User cp = compte("ROLE_CHARGE_PROGRAMME");
        Employe ficheCp = fiche("Chargé de programme", cp, ficheRp);
        MissionResponse m = service.creer(demande(false, ficheCp), cp);
        assertThrows(AccessDeniedException.class, () -> service.rejeter(m.id(), "Non", compte("ROLE_RAF")));
        assertThrows(BusinessException.class, () -> service.rejeter(m.id(), " ", rp));
        assertEquals(StatutMission.REJETEE, service.rejeter(m.id(), "Pas prioritaire", rp).statut());
    }

    // ── Logistique ────────────────────────────────────────

    @Test
    void laLogistiqueAffecteUnVehiculeEtUnChauffeurLibres() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        Employe chauffeur = fiche("Chauffeur", null, null);
        Vehicule hilux = vehicule(EtatMateriel.BON_ETAT);
        MissionResponse m = service.creer(demande(true, agent), raf);
        MissionResponse o = service.organiser(m.id(),
                new OrganisationRequest(hilux.getId(), chauffeur.getId(), "Glacière remise"), compte("ROLE_RESPONSABLE_LOGISTIQUE"));
        assertEquals(StatutMission.ORGANISEE, o.statut());
        assertEquals(chauffeur.getId(), o.chauffeurId());
    }

    @Test
    void unVehiculeDejaPrisOuEnReparationEstRefuse() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        Employe chauffeur = fiche("Chauffeur", null, null);
        Vehicule enReparation = vehicule(EtatMateriel.EN_REPARATION);
        Vehicule pris = vehicule(EtatMateriel.BON_ETAT);
        Mission autre = new Mission();
        autre.setId(UUID.randomUUID());
        autre.setVehiculeId(pris.getId());
        autresMissions.add(autre);
        MissionResponse m = service.creer(demande(true, agent), raf);
        User log = compte("ROLE_RESPONSABLE_LOGISTIQUE");
        assertThrows(BusinessException.class, () -> service.organiser(m.id(),
                new OrganisationRequest(enReparation.getId(), chauffeur.getId(), null), log));
        assertThrows(BusinessException.class, () -> service.organiser(m.id(),
                new OrganisationRequest(pris.getId(), chauffeur.getId(), null), log));
    }

    @Test
    void seulUnChauffeurPeutConduire() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        Employe comptable = fiche("Comptable", null, null);
        Vehicule hilux = vehicule(EtatMateriel.BON_ETAT);
        MissionResponse m = service.creer(demande(true, agent), raf);
        assertThrows(BusinessException.class, () -> service.organiser(m.id(),
                new OrganisationRequest(hilux.getId(), comptable.getId(), null), compte("ROLE_RESPONSABLE_LOGISTIQUE")));
    }

    @Test
    void leVehiculeEstObligatoireSiLaMissionEnDemandeUn() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = service.creer(demande(true, agent), raf);
        assertThrows(BusinessException.class, () -> service.organiser(m.id(),
                new OrganisationRequest(null, null, null), compte("ROLE_RESPONSABLE_LOGISTIQUE")));
    }

    @Test
    void lePosteChauffeurEstReconnuSansAccentNiMajuscule() {
        Employe e = new Employe();
        e.setPoste("CHAUFFEUR principal");
        assertTrue(MissionService.estChauffeur(e));
        e.setPoste("Comptable");
        assertTrue(!MissionService.estChauffeur(e));
    }

    // ── Budget et validations ─────────────────────────────

    @Test
    void sousLeSeuilLeRafValideEtLaComptabiliteSuit() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        MissionResponse p = service.preparer(m.id(), budget(agent, "25000", false), raf);
        assertEquals(0, new BigDecimal("100000").compareTo(p.budgetEstime()));
        assertEquals(StatutMission.VALIDEE, service.validerRaf(m.id(), raf).statut());
    }

    @Test
    void auDelaDuSeuilLeSeValide() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        service.preparer(m.id(), budget(agent, "125000", false), raf); // 4 × 125 000 = 500 000
        assertEquals(StatutMission.EN_VALIDATION_SE, service.validerRaf(m.id(), raf).statut());
        assertEquals(StatutMission.VALIDEE, service.validerSe(m.id(), compte("ROLE_SE")).statut());
    }

    @Test
    void leRafPeutSoumettreAuSeSousLeSeuil() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        service.preparer(m.id(), budget(agent, "1000", true), raf);
        assertEquals(StatutMission.EN_VALIDATION_SE, service.validerRaf(m.id(), raf).statut());
    }

    @Test
    void sansFraisLaMissionEstPreteDesLaValidation() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        service.preparer(m.id(), new PreparationRequest(List.of(agent.getId()), null, List.of(), false), raf);
        assertEquals(StatutMission.PRETE, service.validerRaf(m.id(), raf).statut());
    }

    @Test
    void uneLigneDeBudgetSansQuantiteEstRefusee() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        PreparationRequest r = new PreparationRequest(List.of(agent.getId()), null,
                List.of(new FraisRequest(CategorieFrais.CARBURANT, null, BigDecimal.ZERO, new BigDecimal("700"))), false);
        assertThrows(BusinessException.class, () -> service.preparer(m.id(), r, raf));
    }

    @Test
    void leChefDeMissionDoitFairePartieDeLEquipe() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        Employe autre = fiche("Animateur", null, null);
        MissionResponse m = chezLeRaf(raf, agent);
        PreparationRequest r = new PreparationRequest(List.of(agent.getId()), autre.getId(), List.of(), false);
        assertThrows(BusinessException.class, () -> service.preparer(m.id(), r, raf));
    }

    // ── Fonds et annulation ───────────────────────────────

    @Test
    void lesFondsVontAuChefDeMission() {
        User raf = compte("ROLE_RAF");
        Employe ficheRaf = fiche("RAF", raf, null);
        User cp = compte("ROLE_CHARGE_PROGRAMME");
        Employe chef = fiche("Chargé de programme", cp, null);
        MissionResponse m = chezLeRaf(raf, ficheRaf);
        service.preparer(m.id(), new PreparationRequest(List.of(ficheRaf.getId(), chef.getId()), chef.getId(),
                List.of(new FraisRequest(CategorieFrais.CARBURANT, null, new BigDecimal("50"), new BigDecimal("990"))),
                false), raf);
        service.validerRaf(m.id(), raf);
        UUID idAvance = UUID.randomUUID();
        when(avances.creerAvance(any())).thenReturn(new AvanceResponse(idAvance, null, null, null, null, null, null,
                null, null, null, null, 0, null));
        MissionResponse pret = service.debloquerFonds(m.id(), compte("ROLE_COMPTABLE"));
        ArgumentCaptor<AvanceRequest> avance = ArgumentCaptor.forClass(AvanceRequest.class);
        verify(avances).creerAvance(avance.capture());
        assertEquals(cp.getId(), avance.getValue().beneficiaireId());
        assertEquals(0, new BigDecimal("49500").compareTo(avance.getValue().montant()));
        assertEquals(StatutMission.PRETE, pret.statut());
        assertEquals(idAvance, pret.avanceId());
    }

    @Test
    void uneMissionPreteNeSAnnulePlus() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        service.validerRaf(m.id(), raf); // sans frais : prête
        assertThrows(BusinessException.class, () -> service.annuler(m.id(), null, raf));
    }

    @Test
    void unAutreEmployeNAnnulePasLaMission() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        assertThrows(AccessDeniedException.class, () -> service.annuler(m.id(), null, compte("ROLE_CHARGE_PROGRAMME")));
        verify(avances, never()).creerAvance(any());
    }

    @Test
    void unEmployeNeVoitQueLesMissionsQuiLeConcernent() {
        User raf = compte("ROLE_RAF");
        Employe agent = fiche("RAF", raf, null);
        MissionResponse m = chezLeRaf(raf, agent);
        autresMissions.add(missions.findById(m.id()).orElseThrow());
        User etranger = compte("ROLE_CHARGE_PROGRAMME");
        assertTrue(service.lister(etranger).isEmpty());
        assertEquals(1, service.lister(raf).size());
        assertThrows(AccessDeniedException.class, () -> service.obtenir(m.id(), etranger));
    }
}
