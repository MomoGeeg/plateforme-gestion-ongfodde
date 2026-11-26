package ong.fodde.modules.projets.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.acquisitions.dto.AcquisitionRequest;
import ong.fodde.modules.acquisitions.dto.AcquisitionResponse;
import ong.fodde.modules.acquisitions.entity.Acquisition;
import ong.fodde.modules.acquisitions.entity.CategorieAcquisition;
import ong.fodde.modules.acquisitions.entity.StatutAcquisition;
import ong.fodde.modules.acquisitions.repository.AcquisitionRepository;
import ong.fodde.modules.acquisitions.service.AcquisitionService;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.projets.dto.SuiviDtos.DecisionRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.MembreEquipe;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutActivite;
import ong.fodde.modules.projets.entity.StatutSuivi;
import ong.fodde.modules.projets.entity.SuiviMensuel;
import ong.fodde.modules.projets.entity.TypeActivite;
import ong.fodde.modules.projets.entity.TypeSuivi;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.SuiviMensuelRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Espace superviseur : planning et rapport mensuels, équipe ; achats lancés par le coordonnateur. */
class EspaceSuperviseurTest {

    private final PerimetreRepository perimetres = mock(PerimetreRepository.class);
    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AcquisitionRepository acquisitions = mock(AcquisitionRepository.class);
    private final ActiviteRepository activites = mock(ActiviteRepository.class);
    private final SuiviMensuelRepository suivis = mock(SuiviMensuelRepository.class);
    private final AccesPersonnel acces = new AccesPersonnel(mock(EmployeRepository.class));
    private final AcquisitionService acquisitionService = new AcquisitionService(acquisitions, users, projets,
            mock(HistoriqueService.class), acces, perimetres);
    private final SuiviTerrainService suiviService = new SuiviTerrainService(suivis, perimetres, activites, users,
            mock(ong.fodde.modules.projets.repository.AnimateurProjetRepository.class), projets, new DroitsProjet(acces));

    private User technicien;
    private User superviseur;
    private User autre;
    private User coordonnateur;
    private Perimetre perimetre;
    private final UUID projetId = UUID.randomUUID();
    private final String moisCourant = YearMonth.now().toString();

    @BeforeEach
    void preparer() {
        technicien = compte("Mballo");
        superviseur = compte("Diallo");
        autre = compte("Sow");
        coordonnateur = compte("Badji");
        perimetre = new Perimetre();
        perimetre.setId(UUID.randomUUID());
        perimetre.setProjetId(projetId);
        perimetre.setNom("Périmètre de Saré Yoba");
        perimetre.setTechnicienId(technicien.getId());
        perimetre.setSuperviseurId(superviseur.getId());
        when(perimetres.findById(perimetre.getId())).thenReturn(Optional.of(perimetre));
        when(perimetres.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(technicien.getId())).thenReturn(List.of(perimetre));
        when(perimetres.findBySuperviseurIdAndIsDeletedFalseOrderByNomAsc(superviseur.getId())).thenReturn(List.of(perimetre));
        Projet projet = new Projet();
        projet.setId(projetId);
        projet.setCode("PROJ-2026-002");
        projet.setChefProjetId(coordonnateur.getId());
        when(projets.findById(projetId)).thenReturn(Optional.of(projet));
        when(acquisitions.save(any())).thenAnswer(i -> {
            Acquisition a = i.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            when(acquisitions.findById(a.getId())).thenReturn(Optional.of(a));
            return a;
        });
        when(suivis.save(any())).thenAnswer(i -> {
            SuiviMensuel s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            when(suivis.findById(s.getId())).thenReturn(Optional.of(s));
            when(suivis.findByTechnicienIdAndTypeAndMoisAndIsDeletedFalse(s.getTechnicienId(), s.getType(), s.getMois()))
                    .thenReturn(Optional.of(s));
            return s;
        });
        when(suivis.findByTechnicienIdAndTypeAndMoisAndIsDeletedFalse(any(), any(), any())).thenReturn(Optional.empty());
    }

    private User compte(String nom) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom(nom);
        u.setPrenom("Aliou");
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    private AcquisitionResponse achatPar(User demandeur) {
        return acquisitionService.creerAcquisition(new AcquisitionRequest("Arrosoirs", "Pour le périmètre",
                CategorieAcquisition.MATERIEL_AGRICOLE, null, new BigDecimal("75000"), LocalDate.of(2026, 12, 1),
                null, perimetre.getId()), demandeur);
    }

    // Achats

    @Test
    void leTechnicienEtLeSuperviseurNeLancentPasDAchat() {
        assertThrows(AccessDeniedException.class, () -> achatPar(technicien));
        assertThrows(AccessDeniedException.class, () -> achatPar(superviseur));
    }

    @Test
    void lAchatDuCoordonnateurVaAuRaf() {
        assertEquals(StatutAcquisition.EN_ATTENTE, achatPar(coordonnateur).statut());
    }

    @Test
    void sansChefDeProjetPersonneNeLanceDAchat() {
        projets.findById(projetId).orElseThrow().setChefProjetId(null);
        assertThrows(AccessDeniedException.class, () -> achatPar(coordonnateur));
    }

    @Test
    void leRafValideDirectement() {
        UUID id = achatPar(coordonnateur).id();
        assertEquals(StatutAcquisition.VALIDEE, acquisitionService.validerAcquisition(id, autre.getId()).statut());
    }

    @Test
    void leRafTransmetAuRpQuiValide() {
        UUID id = achatPar(coordonnateur).id();
        assertEquals(StatutAcquisition.ATTENTE_RP, acquisitionService.transmettreAuRp(id, autre.getId()).statut());
        assertThrows(BusinessException.class, () -> acquisitionService.validerAcquisition(id, autre.getId()));
        assertEquals(StatutAcquisition.VALIDEE, acquisitionService.deciderRp(id, true, null, autre.getId()).statut());
    }

    @Test
    void leRejetDuRpExigeUnMotif() {
        UUID id = achatPar(coordonnateur).id();
        acquisitionService.transmettreAuRp(id, autre.getId());
        assertThrows(BusinessException.class, () -> acquisitionService.deciderRp(id, false, " ", autre.getId()));
        assertEquals(StatutAcquisition.REJETEE,
                acquisitionService.deciderRp(id, false, "Budget épuisé", autre.getId()).statut());
    }

    @Test
    void leRpNeDecidePasUnAchatNonTransmis() {
        UUID id = achatPar(coordonnateur).id();
        assertThrows(BusinessException.class, () -> acquisitionService.deciderRp(id, true, null, autre.getId()));
    }

    // Planning et rapport

    @Test
    void lePlanningEstSoumisAvecLesActivitesDuMois() {
        Activite dansLeMois = activite("Repiquage", YearMonth.now().atDay(10), StatutActivite.PLANIFIEE, TypeActivite.ACTIVITE);
        Activite autreMois = activite("Récolte", YearMonth.now().plusMonths(2).atDay(1), StatutActivite.PLANIFIEE,
                TypeActivite.ACTIVITE);
        when(activites.findByResponsableIdAndIsDeletedFalse(technicien.getId())).thenReturn(List.of());
        when(activites.findByPerimetreIdInAndIsDeletedFalse(anyCollection())).thenReturn(List.of(dansLeMois, autreMois));

        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.PLANNING, moisCourant, "Repiquage semaine 2"),
                technicien);
        assertEquals(StatutSuivi.SOUMIS, r.statut());
        assertEquals(List.of("Repiquage"), r.activites().stream().map(a -> a.titre()).toList());
    }

    @Test
    void uneNouvelleRemiseRemplaceLaPrecedenteNonValidee() {
        SuiviResponse premier = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Version 1"), technicien);
        suiviService.decider(premier.id(), new DecisionRequest(false, "Ajoutez les effectifs"), superviseur);
        SuiviResponse second = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Version 2"), technicien);
        assertEquals(premier.id(), second.id());
        assertEquals(StatutSuivi.SOUMIS, second.statut());
        assertEquals("Version 2", second.contenu());
    }

    @Test
    void unDocumentViseNeSeRemetPlus() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Rapport"), technicien);
        assertEquals(StatutSuivi.TRANSMIS, suiviService.decider(r.id(), new DecisionRequest(true, null), superviseur).statut());
        assertThrows(BusinessException.class,
                () -> suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Encore"), technicien));
    }

    // Circuit du rapport : superviseur, puis coordonnateur, puis diffusion

    @Test
    void leSuperviseurTransmetAuCoordonnateurQuiValide() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Rapport"), technicien);
        SuiviMensuel enBase = suivis.findById(r.id()).orElseThrow();
        when(suivis.findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(anyCollection())).thenReturn(List.of(enBase));
        assertTrue(suiviService.suivisEquipe(superviseur).get(0).aDecider());

        SuiviResponse vise = suiviService.decider(r.id(), new DecisionRequest(true, null), superviseur);
        assertEquals(StatutSuivi.TRANSMIS, vise.statut());
        assertEquals("Aliou Diallo", vise.viseParNom());
        assertNotNull(vise.dateVisa());
        assertThrows(AccessDeniedException.class,
                () -> suiviService.decider(r.id(), new DecisionRequest(true, null), superviseur));

        SuiviResponse valide = suiviService.decider(r.id(), new DecisionRequest(true, null), coordonnateur);
        assertEquals(StatutSuivi.VALIDE, valide.statut());
        assertEquals("Aliou Badji", valide.decideParNom());
        assertEquals(List.of("PROJ-2026-002"), valide.projets());
        assertThrows(BusinessException.class,
                () -> suiviService.decider(r.id(), new DecisionRequest(true, null), coordonnateur));
    }

    @Test
    void leCoordonnateurNeDecidePasAvantLeSuperviseur() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Rapport"), technicien);
        assertThrows(AccessDeniedException.class,
                () -> suiviService.decider(r.id(), new DecisionRequest(true, null), coordonnateur));
    }

    @Test
    void leCoordonnateurVoitLesRapportsDesTechniciensDeSesProjets() {
        Projet projet = projets.findById(projetId).orElseThrow();
        when(projets.findByChefProjetId(coordonnateur.getId())).thenReturn(List.of(projet));
        when(perimetres.findByProjetIdAndIsDeletedFalseOrderByNomAsc(projetId)).thenReturn(List.of(perimetre));
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Rapport"), technicien);
        SuiviMensuel enBase = suivis.findById(r.id()).orElseThrow();
        when(suivis.findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(anyCollection())).thenReturn(List.of(enBase));

        assertFalse(suiviService.suivisEquipe(coordonnateur).get(0).aDecider());
        suiviService.decider(r.id(), new DecisionRequest(true, null), superviseur);
        assertTrue(suiviService.suivisEquipe(coordonnateur).get(0).aDecider());
    }

    @Test
    void leCoordonnateurRenvoieALAuteurAvecUnCommentaire() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Version 1"), technicien);
        suiviService.decider(r.id(), new DecisionRequest(true, null), superviseur);
        assertThrows(BusinessException.class,
                () -> suiviService.decider(r.id(), new DecisionRequest(false, " "), coordonnateur));

        SuiviResponse renvoye = suiviService.decider(r.id(), new DecisionRequest(false, "Ajoutez les effectifs"), coordonnateur);
        assertEquals(StatutSuivi.A_REVOIR, renvoye.statut());
        assertEquals("Ajoutez les effectifs", suiviService.mesSuivis(technicienAvecSuivis(r)).get(0).commentaire());

        SuiviResponse remis = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Version 2"), technicien);
        assertEquals(StatutSuivi.SOUMIS, remis.statut());
        assertNull(remis.viseParNom());
        assertNull(remis.commentaire());
    }

    @Test
    void leSuperviseurCorrigeLeTexteEnLeVisant() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Texte du technicien"),
                technicien);
        SuiviResponse vise = suiviService.decider(r.id(),
                new DecisionRequest(true, null, "Texte corrigé par le superviseur"), superviseur);
        assertEquals("Texte corrigé par le superviseur", vise.contenu());
        assertEquals("Texte du technicien", vise.contenuInitial());
    }

    @Test
    void sansCoordonnateurLeVisaDuSuperviseurSuffit() {
        projets.findById(projetId).orElseThrow().setChefProjetId(null);
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Rapport"), technicien);
        assertEquals(StatutSuivi.VALIDE, suiviService.decider(r.id(), new DecisionRequest(true, null), superviseur).statut());
    }

    @Test
    void lesDocumentsValidesSontDiffuses() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, moisCourant, "Rapport"), technicien);
        suiviService.decider(r.id(), new DecisionRequest(true, null), superviseur);
        suiviService.decider(r.id(), new DecisionRequest(true, null), coordonnateur);
        SuiviMensuel enBase = suivis.findById(r.id()).orElseThrow();
        when(suivis.findByStatutAndIsDeletedFalseOrderByDateDecisionDesc(StatutSuivi.VALIDE)).thenReturn(List.of(enBase));
        List<SuiviResponse> diffuses = suiviService.valides(autre);
        assertEquals(1, diffuses.size());
        assertFalse(diffuses.get(0).aDecider());
    }

    private User technicienAvecSuivis(SuiviResponse r) {
        SuiviMensuel enBase = suivis.findById(r.id()).orElseThrow();
        when(suivis.findByTechnicienIdAndIsDeletedFalseOrderByMoisDescTypeAsc(technicien.getId())).thenReturn(List.of(enBase));
        return technicien;
    }

    @Test
    void leRapportDUnMoisAVenirEstRefuse() {
        String prochain = YearMonth.now().plusMonths(1).toString();
        assertThrows(BusinessException.class,
                () -> suiviService.soumettre(new SuiviRequest(TypeSuivi.RAPPORT, prochain, "Trop tôt"), technicien));
    }

    @Test
    void unTechnicienSansPerimetreNeRemetRien() {
        when(perimetres.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(autre.getId())).thenReturn(List.of());
        assertThrows(BusinessException.class,
                () -> suiviService.soumettre(new SuiviRequest(TypeSuivi.PLANNING, moisCourant, "Plan"), autre));
    }

    @Test
    void seulLeSuperviseurDuTechnicienDecide() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.PLANNING, moisCourant, "Plan"), technicien);
        assertThrows(AccessDeniedException.class,
                () -> suiviService.decider(r.id(), new DecisionRequest(true, null), autre));
    }

    @Test
    void unRenvoiARevoirExigeUnCommentaire() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.PLANNING, moisCourant, "Plan"), technicien);
        assertThrows(BusinessException.class,
                () -> suiviService.decider(r.id(), new DecisionRequest(false, ""), superviseur));
    }

    // Équipe

    @Test
    void leSuperviseurVoitLAvancementDeSesTechniciens() {
        Activite faite = activite("Formation bocashi", LocalDate.now().minusDays(5), StatutActivite.REALISEE,
                TypeActivite.FORMATION);
        faite.setParticipants(25);
        Activite enRetard = activite("Piquetage", LocalDate.now().minusDays(10), StatutActivite.EN_COURS,
                TypeActivite.ACTIVITE);
        Activite annulee = activite("Visite", LocalDate.now(), StatutActivite.ANNULEE, TypeActivite.ACTIVITE);
        when(activites.findByPerimetreIdInAndIsDeletedFalse(anyCollection())).thenReturn(List.of(faite, enRetard, annulee));
        SuiviMensuel planning = new SuiviMensuel();
        planning.setTechnicienId(technicien.getId());
        planning.setType(TypeSuivi.PLANNING);
        planning.setMois(moisCourant);
        planning.setStatut(StatutSuivi.SOUMIS);
        when(suivis.findByTechnicienIdInAndIsDeletedFalseOrderByMoisDescTypeAsc(anyCollection()))
                .thenReturn(List.of(planning));

        List<MembreEquipe> equipe = suiviService.equipe(superviseur);
        assertEquals(1, equipe.size());
        MembreEquipe m = equipe.get(0);
        assertEquals("Aliou Mballo", m.nom());
        assertEquals(List.of("Périmètre de Saré Yoba"), m.perimetres());
        assertEquals(2, m.activitesPrevues());
        assertEquals(1, m.activitesRealisees());
        assertEquals(1, m.enRetard());
        assertEquals(1, m.formations());
        assertEquals(25, m.personnesFormees());
        assertSame(StatutSuivi.SOUMIS, m.planningDuMois());
        assertNull(m.dernierRapport());
    }

    private Activite activite(String titre, LocalDate debut, StatutActivite statut, TypeActivite type) {
        Activite a = new Activite();
        a.setId(UUID.randomUUID());
        a.setProjetId(projetId);
        a.setPerimetreId(perimetre.getId());
        a.setTitre(titre);
        a.setDatePrevuDebut(debut);
        a.setDatePrevuFin(debut.plusDays(1));
        a.setStatut(statut);
        a.setType(type);
        return a;
    }
}
