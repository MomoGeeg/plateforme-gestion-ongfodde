package ong.fodde.modules.projets.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ong.fodde.modules.acquisitions.dto.AcquisitionRequest;
import ong.fodde.modules.acquisitions.dto.AcquisitionResponse;
import ong.fodde.modules.acquisitions.entity.Acquisition;
import ong.fodde.modules.acquisitions.entity.CategorieAcquisition;
import ong.fodde.modules.acquisitions.repository.AcquisitionRepository;
import ong.fodde.modules.acquisitions.service.AcquisitionService;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.historique.service.HistoriqueService;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.ResultatAttenduRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Espace terrain : activités du technicien et du superviseur ; achat pour un périmètre par le coordonnateur. */
class EspaceTerrainTest {

    private final ActiviteRepository activites = mock(ActiviteRepository.class);
    private final PerimetreRepository perimetres = mock(PerimetreRepository.class);
    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AcquisitionRepository acquisitions = mock(AcquisitionRepository.class);
    private final AccesPersonnel acces = new AccesPersonnel(mock(EmployeRepository.class));
    private final ActiviteService activiteService = new ActiviteService(activites, projets,
            mock(ResultatAttenduRepository.class), users, perimetres, mock(ong.fodde.modules.projets.repository.AnimateurProjetRepository.class), mock(ong.fodde.modules.projets.repository.PieceActiviteRepository.class), new DroitsProjet(acces));
    private final AcquisitionService acquisitionService = new AcquisitionService(acquisitions, users, projets,
            mock(HistoriqueService.class), acces, perimetres);

    private User technicien;
    private User superviseur;
    private User coordonnateur;
    private Perimetre perimetre;
    private final UUID projetId = UUID.randomUUID();

    @BeforeEach
    void preparer() {
        technicien = compte();
        superviseur = compte();
        coordonnateur = compte();
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
            a.setId(UUID.randomUUID());
            return a;
        });
    }

    private User compte() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom("Mballo");
        u.setPrenom("Aliou");
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    private Activite activite(String titre, LocalDate debut, UUID responsable, UUID perimetreId) {
        Activite a = new Activite();
        a.setId(UUID.randomUUID());
        a.setProjetId(projetId);
        a.setTitre(titre);
        a.setDatePrevuDebut(debut);
        a.setDatePrevuFin(debut.plusDays(1));
        a.setResponsableId(responsable);
        a.setPerimetreId(perimetreId);
        return a;
    }

    @Test
    void leTechnicienVoitSesActivitesEtCellesDeSesPerimetresSansDoublon() {
        Activite formation = activite("Formation bocashi", LocalDate.of(2026, 12, 1), technicien.getId(), perimetre.getId());
        Activite piquetage = activite("Piquetage", LocalDate.of(2026, 11, 1), null, perimetre.getId());
        Activite ailleurs = activite("Suivi hors périmètre", LocalDate.of(2026, 10, 1), technicien.getId(), null);
        when(activites.findByResponsableIdAndIsDeletedFalse(technicien.getId())).thenReturn(List.of(formation, ailleurs));
        when(activites.findByPerimetreIdInAndIsDeletedFalse(anyCollection())).thenReturn(List.of(formation, piquetage));

        List<ActiviteResponse> mes = activiteService.mesActivites(technicien);
        assertEquals(List.of("Suivi hors périmètre", "Piquetage", "Formation bocashi"),
                mes.stream().map(ActiviteResponse::titre).toList());
    }

    @Test
    void leSuperviseurVoitLesActivitesDesPerimetresQuIlSuit() {
        Activite piquetage = activite("Piquetage", LocalDate.of(2026, 11, 1), technicien.getId(), perimetre.getId());
        when(activites.findByResponsableIdAndIsDeletedFalse(superviseur.getId())).thenReturn(List.of());
        when(activites.findByPerimetreIdInAndIsDeletedFalse(anyCollection())).thenReturn(List.of(piquetage));
        assertEquals(1, activiteService.mesActivites(superviseur).size());
    }

    private AcquisitionRequest achat(UUID projet, UUID perimetreId) {
        return new AcquisitionRequest("Arrosoirs", "Pour le périmètre", CategorieAcquisition.MATERIEL_AGRICOLE,
                projet, new BigDecimal("75000"), LocalDate.of(2026, 12, 1), null, perimetreId);
    }

    @Test
    void unAchatPourUnPerimetreRecoitLeProjetDuPerimetre() {
        AcquisitionResponse r = acquisitionService.creerAcquisition(achat(null, perimetre.getId()), coordonnateur);
        assertEquals(projetId, r.projetId());
        assertEquals(perimetre.getId(), r.perimetreId());
        assertEquals("Périmètre de Saré Yoba", r.perimetreNom());
    }

    @Test
    void unAchatSansProjetNiPerimetreEstRefuse() {
        assertThrows(BusinessException.class,
                () -> acquisitionService.creerAcquisition(achat(null, null), coordonnateur));
    }

    @Test
    void unPerimetreDUnAutreProjetEstRefuse() {
        assertThrows(BusinessException.class,
                () -> acquisitionService.creerAcquisition(achat(UUID.randomUUID(), perimetre.getId()), coordonnateur));
    }

    @Test
    void unAchatClassiquePourUnProjetResteAccepte() {
        AcquisitionResponse r = acquisitionService.creerAcquisition(achat(projetId, null), coordonnateur);
        assertEquals(projetId, r.projetId());
        assertEquals(null, r.perimetreId());
    }
}
