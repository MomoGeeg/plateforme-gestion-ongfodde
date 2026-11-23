package ong.fodde.modules.projets.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.ActiviteRequest;
import ong.fodde.modules.projets.dto.ActiviteResponse;
import ong.fodde.modules.projets.dto.PerimetreDtos.PerimetreRequest;
import ong.fodde.modules.projets.dto.PerimetreDtos.PerimetreResponse;
import ong.fodde.modules.projets.entity.Activite;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.entity.TypeActivite;
import ong.fodde.modules.projets.repository.ActiviteRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.projets.repository.ResultatAttenduRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Périmètres : gestion par le chef de projet ou le RP, affectations, formations rattachées. */
class PerimetreTest {

    private final PerimetreRepository perimetres = mock(PerimetreRepository.class);
    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final ActiviteRepository activites = mock(ActiviteRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DroitsProjet droits = new DroitsProjet(new AccesPersonnel(mock(EmployeRepository.class)));
    private final PerimetreService service = new PerimetreService(perimetres, projets, activites, users, droits);
    private final ActiviteService activiteService = new ActiviteService(activites, projets,
            mock(ResultatAttenduRepository.class), users, perimetres, mock(ong.fodde.modules.projets.repository.AnimateurProjetRepository.class), mock(ong.fodde.modules.projets.repository.PieceActiviteRepository.class), droits);

    private final List<Perimetre> lesPerimetres = new ArrayList<>();
    private Projet projet;
    private User chef;
    private User rp;
    private User technicien;
    private User superviseur;
    private User autre;

    @BeforeEach
    void preparer() {
        chef = compte("ROLE_CHARGE_PROGRAMME");
        rp = compte("ROLE_RESPONSABLE_PROGRAMMES");
        technicien = compte("ROLE_TECHNICIEN_TERRAIN");
        superviseur = compte("ROLE_SUPERVISEUR");
        autre = compte("ROLE_COMPTABLE");
        projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setCode("PROJ-2026-001");
        projet.setTitre("Maraîchage Médina Yoro Foulah");
        projet.setStatut(StatutProjet.ACTIF);
        projet.setChefProjetId(chef.getId());
        projet.setDateDebut(LocalDate.of(2026, 1, 1));
        projet.setDateFin(LocalDate.of(2028, 12, 31));
        when(projets.findById(projet.getId())).thenReturn(Optional.of(projet));
        when(perimetres.save(any())).thenAnswer(i -> {
            Perimetre p = i.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
                lesPerimetres.add(p);
            }
            when(perimetres.findById(p.getId())).thenReturn(Optional.of(p));
            return p;
        });
        when(activites.save(any())).thenAnswer(i -> {
            Activite a = i.getArgument(0);
            if (a.getId() == null) {
                a.setId(UUID.randomUUID());
            }
            when(activites.findById(a.getId())).thenReturn(Optional.of(a));
            return a;
        });
    }

    private User compte(String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom("Diallo");
        u.setPrenom(role);
        u.setStatut(StatutUser.ACTIF);
        Role r = new Role();
        r.setNom(role);
        u.setRoles(new HashSet<>(Set.of(r)));
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        when(users.existsById(u.getId())).thenReturn(true);
        return u;
    }

    private PerimetreRequest demande(UUID technicienId, UUID superviseurId) {
        return new PerimetreRequest(projet.getId(), "Périmètre de Saré Yoba", "Saré Yoba", "Médina Yoro Foulah",
                "Médina Yoro Foulah", new BigDecimal("2.5"), technicienId, superviseurId,
                new BigDecimal("12.6789123"), new BigDecimal("-14.0021"), List.of("Oignon", " Tomate ", "", "Oignon"));
    }

    @Test
    void lePerimetreGardeSesCoordonneesEtSesCultures() {
        PerimetreResponse p = service.creer(demande(null, null), chef);
        assertEquals(new BigDecimal("12.678912"), p.latitude());
        assertEquals(new BigDecimal("-14.002100"), p.longitude());
        assertEquals(List.of("Oignon", "Tomate"), p.cultures());
    }

    @Test
    void desCoordonneesIncompletesOuHorsLimitesSontRefusees() {
        PerimetreRequest seule = new PerimetreRequest(projet.getId(), "X", "Y", null, null, null, null, null,
                new BigDecimal("12.6"), null, null);
        assertThrows(BusinessException.class, () -> service.creer(seule, chef));
        PerimetreRequest hors = new PerimetreRequest(projet.getId(), "X", "Y", null, null, null, null, null,
                new BigDecimal("95"), new BigDecimal("-14"), null);
        assertThrows(BusinessException.class, () -> service.creer(hors, chef));
    }

    @Test
    void laCarteEstReserveeAuRpEtAuSe() {
        service.creer(demande(null, null), chef);
        when(perimetres.findByIsDeletedFalseOrderByNomAsc()).thenReturn(lesPerimetres);
        assertEquals(1, service.carte(rp).size());
        assertEquals(1, service.carte(compte("ROLE_SE")).size());
        assertThrows(AccessDeniedException.class, () -> service.carte(chef));
        assertThrows(AccessDeniedException.class, () -> service.carte(technicien));
    }

    @Test
    void leChefDeProjetCreeUnPerimetreAvecSonTechnicienEtSonSuperviseur() {
        PerimetreResponse p = service.creer(demande(technicien.getId(), superviseur.getId()), chef);
        assertEquals("Saré Yoba", p.village());
        assertEquals(technicien.getId(), p.technicienId());
        assertEquals(superviseur.getId(), p.superviseurId());
        assertEquals("PROJ-2026-001", p.projetCode());
    }

    @Test
    void leRpPeutAussiCreerUnPerimetre() {
        assertEquals("Saré Yoba", service.creer(demande(null, null), rp).village());
    }

    @Test
    void unAutreProfilNePeutPasCreerDePerimetre() {
        assertThrows(AccessDeniedException.class, () -> service.creer(demande(null, null), autre));
        assertThrows(AccessDeniedException.class, () -> service.creer(demande(null, null), technicien));
    }

    @Test
    void leTechnicienDoitAvoirLeRoleTechnicien() {
        assertThrows(BusinessException.class, () -> service.creer(demande(autre.getId(), null), chef));
        assertThrows(BusinessException.class, () -> service.creer(demande(null, technicien.getId()), chef));
    }

    @Test
    void unProjetClotureNeRecoitPlusDePerimetre() {
        projet.setStatut(StatutProjet.CLOTURE);
        assertThrows(BusinessException.class, () -> service.creer(demande(null, null), chef));
    }

    @Test
    void unPerimetreNeChangePasDeProjet() {
        PerimetreResponse p = service.creer(demande(null, null), chef);
        PerimetreRequest ailleurs = new PerimetreRequest(UUID.randomUUID(), "X", "Y", null, null, null, null, null, null, null, null);
        assertThrows(BusinessException.class, () -> service.modifier(p.id(), ailleurs, chef));
    }

    @Test
    void unPerimetreAvecDesActivitesNeSeSupprimePas() {
        PerimetreResponse p = service.creer(demande(null, null), chef);
        when(activites.countByPerimetreIdAndIsDeletedFalse(p.id())).thenReturn(2L);
        assertThrows(BusinessException.class, () -> service.supprimer(p.id(), chef));
    }

    @Test
    void unPerimetreSansActiviteSeSupprime() {
        PerimetreResponse p = service.creer(demande(null, null), chef);
        service.supprimer(p.id(), chef);
        assertTrue(lesPerimetres.get(0).getIsDeleted());
    }

    @Test
    void leTechnicienEtLeSuperviseurRetrouventLeursPerimetres() {
        PerimetreResponse p = service.creer(demande(technicien.getId(), superviseur.getId()), chef);
        when(perimetres.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(technicien.getId()))
                .thenReturn(List.of(lesPerimetres.get(0)));
        when(perimetres.findBySuperviseurIdAndIsDeletedFalseOrderByNomAsc(superviseur.getId()))
                .thenReturn(List.of(lesPerimetres.get(0)));
        assertEquals(p.id(), service.mesPerimetres(technicien).get(0).id());
        assertEquals(p.id(), service.mesPerimetres(superviseur).get(0).id());
    }

    @Test
    void lesIntervenantsSontLesComptesActifsDuBonRole() {
        User inactif = compte("ROLE_TECHNICIEN_TERRAIN");
        inactif.setStatut(StatutUser.INACTIF);
        when(users.findDistinctByRoles_NomOrderByNomAscPrenomAsc("ROLE_TECHNICIEN_TERRAIN"))
                .thenReturn(List.of(technicien, inactif));
        when(users.findDistinctByRoles_NomOrderByNomAscPrenomAsc("ROLE_SUPERVISEUR")).thenReturn(List.of(superviseur));
        var intervenants = service.intervenants();
        assertEquals(1, intervenants.techniciens().size());
        assertEquals(technicien.getId(), intervenants.techniciens().get(0).id());
        assertEquals(1, intervenants.superviseurs().size());
    }

    private ActiviteRequest formation(UUID perimetreId, Integer participants, Integer femmes) {
        return new ActiviteRequest(projet.getId(), null, null, "Formation au compost bocashi", null, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), null,
                TypeActivite.FORMATION, perimetreId, "Saré Yoba");
    }

    @Test
    void uneFormationSurUnPerimetreRevientASonTechnicien() {
        PerimetreResponse p = service.creer(demande(technicien.getId(), superviseur.getId()), chef);
        ActiviteResponse a = activiteService.creerActivite(formation(p.id(), null, null), chef);
        assertEquals(TypeActivite.FORMATION, a.type());
        assertEquals(p.id(), a.perimetreId());
        assertEquals("Périmètre de Saré Yoba", a.perimetreNom());
        assertEquals(technicien.getId(), a.responsableId());
    }

    @Test
    void uneActiviteSansTypeEstUneActiviteOrdinaire() {
        ActiviteRequest r = new ActiviteRequest(projet.getId(), null, null, "Piquetage", null, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), null, null, null, null);
        ActiviteResponse a = activiteService.creerActivite(r, chef);
        assertEquals(TypeActivite.ACTIVITE, a.type());
        assertNull(a.perimetreId());
    }

    @Test
    void lePerimetreDUnAutreProjetEstRefuse() {
        Perimetre ailleurs = new Perimetre();
        ailleurs.setId(UUID.randomUUID());
        ailleurs.setProjetId(UUID.randomUUID());
        when(perimetres.findById(ailleurs.getId())).thenReturn(Optional.of(ailleurs));
        assertThrows(BusinessException.class, () -> activiteService.creerActivite(formation(ailleurs.getId(), null, null), chef));
    }
}
