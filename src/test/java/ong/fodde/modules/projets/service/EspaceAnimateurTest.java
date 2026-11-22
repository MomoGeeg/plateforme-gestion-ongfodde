package ong.fodde.modules.projets.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.ged.service.StockageFichierService;
import ong.fodde.modules.projets.dto.ActiviteRequest;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.dto.SuiviDtos.DecisionRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviRequest;
import ong.fodde.modules.projets.dto.SuiviDtos.SuiviResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.AnimateurProjet;
import ong.fodde.modules.projets.entity.PieceActivite;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.entity.StatutSuivi;
import ong.fodde.modules.projets.entity.SuiviMensuel;
import ong.fodde.modules.projets.entity.TypeActivite;
import ong.fodde.modules.projets.entity.TypePiece;
import ong.fodde.modules.projets.entity.TypeSuivi;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.AnimateurProjetRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.PieceActiviteRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.ResultatAttenduRepository;
import ong.fodde.modules.projets.repository.SuiviMensuelRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Espace animateur : liste de présence, photos, activités sur ses projets, suivi validé par le coordonnateur. */
class EspaceAnimateurTest {

    private final ActiviteRepository activites = mock(ActiviteRepository.class);
    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PerimetreRepository perimetres = mock(PerimetreRepository.class);
    private final AnimateurProjetRepository animateurs = mock(AnimateurProjetRepository.class);
    private final PieceActiviteRepository pieces = mock(PieceActiviteRepository.class);
    private final SuiviMensuelRepository suivis = mock(SuiviMensuelRepository.class);
    private final DroitsProjet droits = new DroitsProjet(new AccesPersonnel(mock(EmployeRepository.class)));
    private final ActiviteService activiteService = new ActiviteService(activites, projets,
            mock(ResultatAttenduRepository.class), users, perimetres, animateurs, pieces, droits);
    private final PieceActiviteService pieceService = new PieceActiviteService(pieces, perimetres, activiteService,
            mock(StockageFichierService.class), droits);
    private final AnimateurService animateurService = new AnimateurService(animateurs, projets, users, droits);
    private final SuiviTerrainService suiviService = new SuiviTerrainService(suivis, perimetres, activites, users,
            animateurs, projets, droits);

    private User animateur;
    private User coordonnateur;
    private User autre;
    private Projet projet;
    private final List<PieceActivite> pieceStock = new ArrayList<>();

    @BeforeEach
    void preparer() {
        animateur = compte("ROLE_ANIMATEUR");
        coordonnateur = compte("ROLE_CHARGE_PROGRAMME");
        autre = compte("ROLE_ANIMATEUR");
        projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setCode("PROJ-2026-003");
        projet.setStatut(StatutProjet.ACTIF);
        projet.setChefProjetId(coordonnateur.getId());
        projet.setDateDebut(LocalDate.of(2026, 1, 1));
        projet.setDateFin(LocalDate.of(2027, 12, 31));
        when(projets.findById(projet.getId())).thenReturn(Optional.of(projet));
        when(projets.findByChefProjetId(coordonnateur.getId())).thenReturn(List.of(projet));
        AnimateurProjet lien = new AnimateurProjet();
        lien.setProjetId(projet.getId());
        lien.setAnimateurId(animateur.getId());
        when(animateurs.existsByProjetIdAndAnimateurIdAndIsDeletedFalse(projet.getId(), animateur.getId())).thenReturn(true);
        when(animateurs.findByAnimateurIdAndIsDeletedFalse(animateur.getId())).thenReturn(List.of(lien));
        when(animateurs.findByProjetIdAndIsDeletedFalse(projet.getId())).thenReturn(List.of(lien));
        when(activites.save(any())).thenAnswer(i -> {
            Activite a = i.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            when(activites.findById(a.getId())).thenReturn(Optional.of(a));
            return a;
        });
        when(pieces.save(any())).thenAnswer(i -> {
            PieceActivite p = i.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
                pieceStock.add(p);
            }
            return p;
        });
        when(pieces.countByActiviteIdAndTypeAndIsDeletedFalse(any(), eq(TypePiece.PHOTO))).thenAnswer(i -> pieceStock
                .stream().filter(p -> p.getType() == TypePiece.PHOTO && !Boolean.TRUE.equals(p.getIsDeleted())).count());
        when(pieces.findByActiviteIdAndTypeAndIsDeletedFalse(any(), eq(TypePiece.LISTE_PRESENCE))).thenAnswer(i -> pieceStock
                .stream().filter(p -> p.getType() == TypePiece.LISTE_PRESENCE && !Boolean.TRUE.equals(p.getIsDeleted()))
                .toList());
        when(suivis.save(any())).thenAnswer(i -> {
            SuiviMensuel s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            when(suivis.findById(s.getId())).thenReturn(Optional.of(s));
            return s;
        });
        when(suivis.findByTechnicienIdAndTypeAndMoisAndIsDeletedFalse(any(), any(), any())).thenReturn(Optional.empty());
    }

    private User compte(String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom("Sow");
        u.setPrenom("Awa");
        Role r = new Role();
        r.setNom(role);
        u.getRoles().add(r);
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        when(users.existsById(u.getId())).thenReturn(true);
        return u;
    }

    private ActiviteRequest causerie(UUID responsable) {
        return new ActiviteRequest(projet.getId(), null, null, "Causerie sur la nutrition", null, responsable,
                LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 25), null, TypeActivite.SENSIBILISATION, null,
                "Quartier Sinthiang");
    }

    private ActiviteResponse causerieDeLAnimateur() {
        return activiteService.creerActivite(causerie(null), animateur);
    }

    private static MockMultipartFile csv(String contenu) {
        return new MockMultipartFile("fichier", "presence.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile photo(String nom) {
        return new MockMultipartFile("fichier", nom, "image/jpeg", new byte[] {1, 2, 3});
    }

    // Liste de présence

    @Test
    void laListeCompteLesParticipantsEtLesFemmes() {
        ListePresence.Comptage c = ListePresence.compter("""
                N°;Prénom et nom;Sexe;Village;Signature
                1;Awa Ba;F;Sinthiang;
                2;Moussa Diallo;M;Sinthiang;

                3;Fatou Sané;Femme;Saré Yoba;
                """.getBytes(StandardCharsets.UTF_8));
        assertEquals(3, c.participants());
        assertEquals(2, c.femmes());
    }

    @Test
    void sansColonneSexeLesFemmesNeSontPasComptees() {
        ListePresence.Comptage c = ListePresence.compter("Nom,Village\nAwa Ba,Sinthiang\n\"Diallo, Moussa\",Kolda\n"
                .getBytes(StandardCharsets.UTF_8));
        assertEquals(2, c.participants());
        assertNull(c.femmes());
    }

    @Test
    void uneListeExcelEnWindows1252EstLue() {
        byte[] contenu = "Nom;Genre\nAïssatou Baldé;Féminin\nOumar Sow;Masculin\n".getBytes(Charset.forName("windows-1252"));
        ListePresence.Comptage c = ListePresence.compter(contenu);
        assertEquals(2, c.participants());
        assertEquals(1, c.femmes());
    }

    @Test
    void uneListeSansParticipantEstRefusee() {
        assertThrows(BusinessException.class, () -> ListePresence.compter("Nom;Sexe\n\n".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void leDepotDeLaListeDonneLesParticipantsDeLActivite() {
        ActiviteResponse a = causerieDeLAnimateur();
        ActiviteResponse apres = pieceService.deposerListePresence(a.id(), csv("Nom;Sexe\nAwa;F\nMoussa;M\n"), animateur);
        assertEquals(2, apres.participants());
        assertEquals(1, apres.participantsFemmes());
    }

    @Test
    void uneNouvelleListeRemplaceLaPrecedente() {
        ActiviteResponse a = causerieDeLAnimateur();
        pieceService.deposerListePresence(a.id(), csv("Nom;Sexe\nAwa;F\nMoussa;M\n"), animateur);
        ActiviteResponse apres = pieceService.deposerListePresence(a.id(), csv("Nom;Sexe\nAwa;F\nMoussa;M\nBinta;F\n"), animateur);
        assertEquals(3, apres.participants());
        assertEquals(1, pieceStock.stream()
                .filter(p -> p.getType() == TypePiece.LISTE_PRESENCE && !Boolean.TRUE.equals(p.getIsDeleted())).count());
    }

    @Test
    void laListeDoitEtreUnCsv() {
        ActiviteResponse a = causerieDeLAnimateur();
        assertThrows(BusinessException.class, () -> pieceService.deposerListePresence(a.id(), photo("liste.jpg"), animateur));
    }

    @Test
    void unAutreAnimateurNeDeposePasDePiece() {
        ActiviteResponse a = causerieDeLAnimateur();
        assertThrows(AccessDeniedException.class,
                () -> pieceService.deposerListePresence(a.id(), csv("Nom\nAwa\n"), autre));
    }

    // Photos

    @Test
    void troisPhotosAuPlusParActivite() {
        ActiviteResponse a = causerieDeLAnimateur();
        pieceService.ajouterPhoto(a.id(), photo("1.jpg"), animateur);
        pieceService.ajouterPhoto(a.id(), photo("2.png"), animateur);
        pieceService.ajouterPhoto(a.id(), photo("3.jpeg"), animateur);
        assertThrows(BusinessException.class, () -> pieceService.ajouterPhoto(a.id(), photo("4.jpg"), animateur));
    }

    // Activités de l'animateur

    @Test
    void lAnimateurPlanifieSesActivitesSurSonProjet() {
        ActiviteResponse a = activiteService.creerActivite(causerie(coordonnateur.getId()), animateur);
        assertEquals(animateur.getId(), a.responsableId());
        assertEquals(TypeActivite.SENSIBILISATION, a.type());
        assertEquals("Quartier Sinthiang", a.lieu());
    }

    @Test
    void unAnimateurHorsDuProjetNePlanifiePas() {
        assertThrows(AccessDeniedException.class, () -> activiteService.creerActivite(causerie(null), autre));
    }

    @Test
    void lAnimateurNeModifiePasLActiviteDUnAutre() {
        ActiviteResponse a = activiteService.creerActivite(causerie(autre.getId()), coordonnateur);
        assertThrows(AccessDeniedException.class, () -> activiteService.modifierActivite(a.id(), causerie(null), animateur));
    }

    @Test
    void lAnimateurRedigeLaFicheEtMarqueLActiviteRealisee() {
        ActiviteResponse a = causerieDeLAnimateur();
        assertEquals("40 mères présentes", activiteService.enregistrerFicheSuivi(a.id(), " 40 mères présentes ", animateur).ficheSuivi());
        assertEquals(100, activiteService.marquerRealisee(a.id(), animateur).tauxAvancement());
    }

    @Test
    void seulUnGestionnaireAffecteUnAnimateur() {
        assertThrows(AccessDeniedException.class, () -> animateurService.affecter(projet.getId(), autre.getId(), animateur));
    }

    @Test
    void unCompteSansLeRoleAnimateurNEstPasAffecte() {
        assertThrows(BusinessException.class,
                () -> animateurService.affecter(projet.getId(), coordonnateur.getId(), coordonnateur));
    }

    // Planning et rapport

    @Test
    void leCoordonnateurValideLePlanningDeLAnimateur() {
        SuiviResponse r = suiviService.soumettre(new SuiviRequest(TypeSuivi.PLANNING, YearMonth.now().toString(),
                "Causeries dans 4 quartiers"), animateur);
        assertThrows(AccessDeniedException.class, () -> suiviService.decider(r.id(), new DecisionRequest(true, null), autre));
        assertEquals(StatutSuivi.VALIDE, suiviService.decider(r.id(), new DecisionRequest(true, null), coordonnateur).statut());
    }

    @Test
    void leCoordonnateurVoitSonAnimateurDansSonEquipe() {
        Activite faite = new Activite();
        faite.setId(UUID.randomUUID());
        faite.setProjetId(projet.getId());
        faite.setResponsableId(animateur.getId());
        faite.setTitre("Causerie");
        faite.setType(TypeActivite.SENSIBILISATION);
        faite.setStatut(ong.fodde.modules.projets.entity.StatutActivite.REALISEE);
        faite.setDatePrevuDebut(LocalDate.now().minusDays(3));
        faite.setDatePrevuFin(LocalDate.now().minusDays(3));
        faite.setParticipants(40);
        when(activites.findByResponsableIdAndIsDeletedFalse(animateur.getId())).thenReturn(List.of(faite));
        var equipe = suiviService.equipe(coordonnateur);
        assertEquals(1, equipe.size());
        assertEquals(List.of("PROJ-2026-003"), equipe.get(0).perimetres());
        assertEquals(40, equipe.get(0).personnesFormees());
        assertEquals(1, equipe.get(0).activitesRealisees());
    }
}
