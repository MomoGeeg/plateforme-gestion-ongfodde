package ong.fodde.modules.projets.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import ong.fodde.modules.auth.entity.Role;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.projets.dto.MesureDtos.IndicateursDuPerimetre;
import ong.fodde.modules.projets.dto.MesureDtos.MesureRequest;
import ong.fodde.modules.projets.entity.MesurePerimetre;
import ong.fodde.modules.projets.entity.Perimetre;
import ong.fodde.modules.projets.entity.Projet;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.repository.MesurePerimetreRepository;
import ong.fodde.modules.projets.repository.PerimetreRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.modules.rh.repository.EmployeRepository;
import ong.fodde.modules.se.entity.Indicateur;
import ong.fodde.modules.se.repository.IndicateurRepository;
import ong.fodde.shared.exception.BusinessException;
import ong.fodde.shared.securite.AccesPersonnel;

/** Indicateurs par périmètre : droits de saisie, contrôles et dernière valeur. */
class MesurePerimetreTest {

    private final MesurePerimetreRepository mesures = mock(MesurePerimetreRepository.class);
    private final PerimetreRepository perimetres = mock(PerimetreRepository.class);
    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final IndicateurRepository indicateurs = mock(IndicateurRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DroitsProjet droits = new DroitsProjet(new AccesPersonnel(mock(EmployeRepository.class)));
    private final MesurePerimetreService service = new MesurePerimetreService(mesures, perimetres, projets,
            indicateurs, users, droits);

    private final List<MesurePerimetre> lesMesures = new ArrayList<>();
    private Projet projet;
    private Perimetre perimetre;
    private Indicateur superficie;
    private Indicateur productrices;
    private User technicien;
    private User superviseur;
    private User suiviEvaluation;
    private User comptable;

    @BeforeEach
    void preparer() {
        technicien = compte("ROLE_TECHNICIEN_TERRAIN");
        superviseur = compte("ROLE_SUPERVISEUR");
        suiviEvaluation = compte("ROLE_RESPONSABLE_SE");
        comptable = compte("ROLE_COMPTABLE");
        projet = new Projet();
        projet.setId(UUID.randomUUID());
        projet.setStatut(StatutProjet.ACTIF);
        when(projets.findById(projet.getId())).thenReturn(Optional.of(projet));
        perimetre = new Perimetre();
        perimetre.setId(UUID.randomUUID());
        perimetre.setProjetId(projet.getId());
        perimetre.setNom("Périmètre de Saré Yoba");
        perimetre.setVillage("Saré Yoba");
        perimetre.setTechnicienId(technicien.getId());
        perimetre.setSuperviseurId(superviseur.getId());
        when(perimetres.findById(perimetre.getId())).thenReturn(Optional.of(perimetre));
        superficie = indicateur("IND-01", "Superficie emblavée", "ha");
        productrices = indicateur("IND-02", "Productrices actives", "personnes");
        when(indicateurs.findByProjetId(projet.getId())).thenReturn(List.of(productrices, superficie));
        when(mesures.save(any())).thenAnswer(i -> {
            MesurePerimetre m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(LocalDateTime.now());
            lesMesures.add(m);
            return m;
        });
        when(mesures.findByPerimetreIdAndIsDeletedFalseOrderByDateMesureDescCreatedAtDesc(perimetre.getId()))
                .thenAnswer(i -> lesMesures.stream()
                        .sorted(Comparator.comparing(MesurePerimetre::getDateMesure).reversed()
                                .thenComparing(MesurePerimetre::getCreatedAt, Comparator.reverseOrder()))
                        .toList());
    }

    private User compte(String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setNom("Diallo");
        u.setPrenom("Awa");
        Role r = new Role();
        r.setNom(role);
        u.setRoles(new HashSet<>(Set.of(r)));
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    private Indicateur indicateur(String code, String libelle, String unite) {
        Indicateur i = new Indicateur();
        i.setId(UUID.randomUUID());
        i.setProjetId(projet.getId());
        i.setCode(code);
        i.setLibelle(libelle);
        i.setUnite(unite);
        i.setValeurCible(new BigDecimal("10"));
        when(indicateurs.findById(i.getId())).thenReturn(Optional.of(i));
        return i;
    }

    private MesureRequest mesure(Indicateur i, LocalDate date, String valeur) {
        return new MesureRequest(i.getId(), date, new BigDecimal(valeur), "Relevé du mois");
    }

    @Test
    void leTechnicienDuPerimetreSaisitUneMesure() {
        var r = service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now(), "2.5"), technicien);
        assertEquals(new BigDecimal("2.5"), r.valeur());
        assertEquals("Awa Diallo", r.saisiParNom());
    }

    @Test
    void leSuperviseurEtLeSuiviEvaluationSaisissentAussi() {
        service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now(), "2"), superviseur);
        service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now(), "3"), suiviEvaluation);
        assertEquals(2, lesMesures.size());
    }

    @Test
    void unAutreCompteNeSaisitPas() {
        assertThrows(AccessDeniedException.class,
                () -> service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now(), "2"), comptable));
        User autreTechnicien = compte("ROLE_TECHNICIEN_TERRAIN");
        assertThrows(AccessDeniedException.class,
                () -> service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now(), "2"), autreTechnicien));
    }

    @Test
    void unIndicateurDUnAutreProjetEstRefuse() {
        Indicateur ailleurs = indicateur("IND-99", "Autre", "u");
        ailleurs.setProjetId(UUID.randomUUID());
        assertThrows(BusinessException.class,
                () -> service.enregistrer(perimetre.getId(), mesure(ailleurs, LocalDate.now(), "1"), technicien));
    }

    @Test
    void uneDateFutureOuUneValeurNegativeSontRefusees() {
        assertThrows(BusinessException.class, () -> service.enregistrer(perimetre.getId(),
                mesure(superficie, LocalDate.now().plusDays(1), "1"), technicien));
        assertThrows(BusinessException.class, () -> service.enregistrer(perimetre.getId(),
                mesure(superficie, LocalDate.now(), "-1"), technicien));
    }

    @Test
    void unProjetClotureNeRecoitPlusDeMesure() {
        projet.setStatut(StatutProjet.CLOTURE);
        assertThrows(BusinessException.class,
                () -> service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now(), "1"), technicien));
    }

    @Test
    void leResumeDonneLaDerniereValeurDeChaqueIndicateur() {
        service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now().minusDays(30), "1.5"), technicien);
        service.enregistrer(perimetre.getId(), mesure(superficie, LocalDate.now(), "2.5"), technicien);
        IndicateursDuPerimetre r = service.indicateurs(perimetre.getId(), technicien);
        assertTrue(r.peutSaisir());
        assertEquals("IND-01", r.indicateurs().get(0).code());
        assertEquals(new BigDecimal("2.5"), r.indicateurs().get(0).derniereValeur());
        assertEquals(2, r.indicateurs().get(0).nbMesures());
        assertNull(r.indicateurs().get(1).derniereValeur());
        assertEquals(0, r.indicateurs().get(1).nbMesures());
    }

    @Test
    void leComptableVoitSansPouvoirSaisir() {
        assertFalse(service.indicateurs(perimetre.getId(), comptable).peutSaisir());
    }

    @Test
    void leTechnicienRetrouveLesIndicateursDeSesPerimetres() {
        when(perimetres.findByTechnicienIdAndIsDeletedFalseOrderByNomAsc(technicien.getId())).thenReturn(List.of(perimetre));
        when(perimetres.findBySuperviseurIdAndIsDeletedFalseOrderByNomAsc(technicien.getId())).thenReturn(List.of());
        List<IndicateursDuPerimetre> r = service.mesIndicateurs(technicien);
        assertEquals(1, r.size());
        assertEquals(2, r.get(0).indicateurs().size());
    }
}
