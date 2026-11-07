package ong.fodde.modules.logistique.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ong.fodde.modules.acquisitions.entity.Acquisition;
import ong.fodde.modules.acquisitions.entity.StatutAcquisition;
import ong.fodde.modules.acquisitions.repository.AcquisitionRepository;
import ong.fodde.modules.auth.repository.UserRepository;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.AlerteVehicule;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.DepartRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MaterielRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MouvementRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.RetourRequest;
import ong.fodde.modules.logistique.entity.CategorieMateriel;
import ong.fodde.modules.logistique.entity.EtatMateriel;
import ong.fodde.modules.logistique.entity.Materiel;
import ong.fodde.modules.logistique.entity.MouvementMateriel;
import ong.fodde.modules.logistique.entity.SortieVehicule;
import ong.fodde.modules.logistique.entity.TypeMouvement;
import ong.fodde.modules.logistique.entity.Vehicule;
import ong.fodde.modules.logistique.repository.MaterielRepository;
import ong.fodde.modules.logistique.repository.MouvementMaterielRepository;
import ong.fodde.modules.logistique.repository.SortieVehiculeRepository;
import ong.fodde.modules.logistique.repository.VehiculeRepository;
import ong.fodde.modules.projets.repository.ProjetRepository;
import ong.fodde.shared.exception.BusinessException;

/** Règles de la logistique : inventaire, mouvements, alertes et carnet de bord. */
class LogistiqueTest {

    private final MaterielRepository materiels = mock(MaterielRepository.class);
    private final MouvementMaterielRepository mouvements = mock(MouvementMaterielRepository.class);
    private final AcquisitionRepository acquisitions = mock(AcquisitionRepository.class);
    private final ProjetRepository projets = mock(ProjetRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MaterielService materielService =
            new MaterielService(materiels, mouvements, acquisitions, projets, users);

    private final VehiculeRepository vehicules = mock(VehiculeRepository.class);
    private final SortieVehiculeRepository sorties = mock(SortieVehiculeRepository.class);
    private final VehiculeService vehiculeService = new VehiculeService(vehicules, sorties, projets, users);

    {
        when(materiels.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mouvements.save(any())).thenAnswer(i -> i.getArgument(0));
        when(vehicules.save(any())).thenAnswer(i -> i.getArgument(0));
        when(sorties.save(any())).thenAnswer(i -> i.getArgument(0));
        when(materiels.prochainCodeActif()).thenReturn("MAT-2026-001");
        when(users.existsById(any())).thenReturn(true);
        when(projets.existsById(any())).thenReturn(true);
    }

    // ── Inventaire ──────────────────────────────────────────────────────────

    private static MaterielRequest fiche(UUID acquisitionId, UUID responsableId) {
        return new MaterielRequest(null, "Ordinateur portable", CategorieMateriel.INFORMATIQUE, "Dell Latitude",
                null, new BigDecimal("450000"), null, null, null, null, null, responsableId, acquisitionId);
    }

    private Acquisition acquisition(StatutAcquisition statut) {
        Acquisition a = new Acquisition();
        a.setId(UUID.randomUUID());
        a.setStatut(statut);
        a.setProjetId(UUID.randomUUID());
        when(acquisitions.findById(a.getId())).thenReturn(Optional.of(a));
        when(materiels.findByAcquisitionId(a.getId())).thenReturn(Optional.empty());
        return a;
    }

    @Test
    void unBienCreeRecoitUnCodeEtUnPremierMouvement() {
        var r = materielService.creer(fiche(null, null), UUID.randomUUID());
        assertEquals("MAT-2026-001", r.codeActif());
        assertEquals(EtatMateriel.BON_ETAT, r.etat());
        ArgumentCaptor<MouvementMateriel> c = ArgumentCaptor.forClass(MouvementMateriel.class);
        verify(mouvements).save(c.capture());
        assertEquals(TypeMouvement.INVENTAIRE, c.getValue().getType());
    }

    @Test
    void uneAcquisitionValideeEntreDansLInventaireAvecSonProjet() {
        Acquisition a = acquisition(StatutAcquisition.VALIDEE);
        var r = materielService.creer(fiche(a.getId(), null), UUID.randomUUID());
        assertEquals(a.getId(), r.acquisitionId());
        assertEquals(a.getProjetId(), r.projetId());
    }

    @Test
    void uneAcquisitionNonValideeEstRefusee() {
        Acquisition a = acquisition(StatutAcquisition.EN_ATTENTE);
        assertThrows(BusinessException.class, () -> materielService.creer(fiche(a.getId(), null), UUID.randomUUID()));
    }

    @Test
    void uneAcquisitionDejaInventorieeEstRefusee() {
        Acquisition a = acquisition(StatutAcquisition.VALIDEE);
        Materiel deja = new Materiel();
        deja.setCodeActif("MAT-2026-007");
        when(materiels.findByAcquisitionId(a.getId())).thenReturn(Optional.of(deja));
        var ex = assertThrows(BusinessException.class, () -> materielService.creer(fiche(a.getId(), null), UUID.randomUUID()));
        assertTrue(ex.getMessage().contains("MAT-2026-007"));
    }

    // ── Mouvements ──────────────────────────────────────────────────────────

    private Materiel bien(EtatMateriel etat, UUID responsableId) {
        Materiel m = new Materiel();
        m.setId(UUID.randomUUID());
        m.setCodeActif("MAT-2026-002");
        m.setEtat(etat);
        m.setResponsableId(responsableId);
        when(materiels.findById(m.getId())).thenReturn(Optional.of(m));
        return m;
    }

    private static MouvementRequest mvt(TypeMouvement type, UUID responsable, EtatMateriel etatApres, String notes) {
        return new MouvementRequest(type, null, responsable, null, null, etatApres, notes);
    }

    @Test
    void lAffectationChangeLeResponsable() {
        Materiel m = bien(EtatMateriel.BON_ETAT, null);
        UUID awa = UUID.randomUUID();
        materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.AFFECTATION, awa, null, null), UUID.randomUUID());
        assertEquals(awa, m.getResponsableId());
    }

    @Test
    void lAffectationSansPersonneNiProjetEstRefusee() {
        Materiel m = bien(EtatMateriel.BON_ETAT, null);
        assertThrows(BusinessException.class,
                () -> materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.AFFECTATION, null, null, null), null));
    }

    @Test
    void leRetourLibereLeBienEtLeRemetAuMagasin() {
        Materiel m = bien(EtatMateriel.BON_ETAT, UUID.randomUUID());
        materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.RETOUR, null, EtatMateriel.USE, null), null);
        assertNull(m.getResponsableId());
        assertEquals(EtatMateriel.USE, m.getEtat());
        assertEquals("Magasin", m.getLocalisation());
    }

    @Test
    void leRetourDeReparationExigeLEtat() {
        Materiel m = bien(EtatMateriel.EN_REPARATION, null);
        assertThrows(BusinessException.class,
                () -> materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.RETOUR, null, null, null), null));
        materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.RETOUR, null, EtatMateriel.BON_ETAT, null), null);
        assertEquals(EtatMateriel.BON_ETAT, m.getEtat());
    }

    @Test
    void laMiseHorsServiceExigeUneRaisonPuisBloqueToutMouvement() {
        Materiel m = bien(EtatMateriel.USE, UUID.randomUUID());
        assertThrows(BusinessException.class,
                () -> materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.MISE_HORS_SERVICE, null, null, " "), null));
        materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.MISE_HORS_SERVICE, null, null, "Écran cassé"), null);
        assertEquals(EtatMateriel.HORS_SERVICE, m.getEtat());
        assertNull(m.getResponsableId());
        assertThrows(BusinessException.class, () -> materielService.enregistrerMouvement(m.getId(),
                mvt(TypeMouvement.AFFECTATION, UUID.randomUUID(), null, null), null));
    }

    @Test
    void unMouvementEnregistreLEtatAvantEtApres() {
        Materiel m = bien(EtatMateriel.BON_ETAT, null);
        var r = materielService.enregistrerMouvement(m.getId(), mvt(TypeMouvement.REPARATION, null, null, null), null);
        assertEquals("BON_ETAT", r.etatAvant());
        assertEquals("EN_REPARATION", r.etatApres());
    }

    @Test
    void unMouvementDateDansLeFuturEstRefuse() {
        Materiel m = bien(EtatMateriel.BON_ETAT, null);
        var req = new MouvementRequest(TypeMouvement.INVENTAIRE, LocalDate.now().plusDays(2), null, null, null,
                EtatMateriel.BON_ETAT, null);
        assertThrows(BusinessException.class, () -> materielService.enregistrerMouvement(m.getId(), req, null));
        verify(mouvements, never()).save(any());
    }

    // ── Alertes véhicules ───────────────────────────────────────────────────

    private static Vehicule vehicule(int km) {
        Vehicule v = new Vehicule();
        v.setId(UUID.randomUUID());
        v.setImmatriculation("DK 1234 AB");
        v.setMarqueModele("Toyota Hilux");
        v.setKilometrageActuel(km);
        return v;
    }

    @Test
    void lesAlertesSignalentEcheancesEtRevision() {
        LocalDate jour = LocalDate.of(2026, 11, 8);
        Vehicule v = vehicule(48_700);
        v.setDateExpirationAssurance(jour.minusDays(1));
        v.setDateVisiteTechnique(jour.plusDays(10));
        v.setKilometrageRevision(49_000);
        List<String> codes = VehiculeService.alertes(v, jour).stream().map(AlerteVehicule::code).toList();
        assertEquals(List.of("ASSURANCE_EXPIREE", "VISITE_PROCHE", "REVISION_PROCHE"), codes);
    }

    @Test
    void pasDAlerteLoinDesEcheancesNiPourUnVehiculeHorsService() {
        LocalDate jour = LocalDate.of(2026, 11, 8);
        Vehicule v = vehicule(10_000);
        v.setDateExpirationAssurance(jour.plusDays(90));
        v.setKilometrageRevision(20_000);
        assertTrue(VehiculeService.alertes(v, jour).isEmpty());
        v.setDateExpirationAssurance(jour.minusDays(5));
        v.setEtat(EtatMateriel.HORS_SERVICE);
        assertTrue(VehiculeService.alertes(v, jour).isEmpty());
    }

    // ── Carnet de bord ──────────────────────────────────────────────────────

    private Vehicule auGarage(int km) {
        Vehicule v = vehicule(km);
        when(vehicules.findById(v.getId())).thenReturn(Optional.of(v));
        when(sorties.findFirstByVehiculeIdAndDateRetourIsNull(v.getId())).thenReturn(Optional.empty());
        return v;
    }

    @Test
    void leDepartPartDuKilometrageDuVehicule() {
        Vehicule v = auGarage(12_000);
        var s = vehiculeService.depart(v.getId(), new DepartRequest(UUID.randomUUID(), "Vélingara", null, null, null, null), null);
        assertEquals(12_000, s.kmDepart());
    }

    @Test
    void unVehiculeDejaSortiNePeutPasRepartir() {
        Vehicule v = auGarage(12_000);
        when(sorties.findFirstByVehiculeIdAndDateRetourIsNull(v.getId())).thenReturn(Optional.of(new SortieVehicule()));
        assertThrows(BusinessException.class, () -> vehiculeService.depart(v.getId(),
                new DepartRequest(UUID.randomUUID(), "Sédhiou", null, null, null, null), null));
    }

    @Test
    void unKilometrageDeDepartInferieurEstRefuse() {
        Vehicule v = auGarage(12_000);
        assertThrows(BusinessException.class, () -> vehiculeService.depart(v.getId(),
                new DepartRequest(UUID.randomUUID(), "Sédhiou", null, null, 11_500, null), null));
    }

    private SortieVehicule enCours(Vehicule v, int kmDepart) {
        SortieVehicule s = new SortieVehicule();
        s.setId(UUID.randomUUID());
        s.setVehiculeId(v.getId());
        s.setKmDepart(kmDepart);
        s.setDateDepart(LocalDateTime.now().minusHours(3));
        when(sorties.findById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void leRetourMetAJourLeKilometrageEtLaDistance() {
        Vehicule v = auGarage(12_000);
        SortieVehicule s = enCours(v, 12_000);
        var r = vehiculeService.retour(s.getId(), new RetourRequest(12_184, null, "RAS"));
        assertEquals(184, r.distance());
        assertEquals(12_184, v.getKilometrageActuel());
    }

    @Test
    void unRetourAvecMoinsDeKilometresQuAuDepartEstRefuse() {
        Vehicule v = auGarage(12_000);
        SortieVehicule s = enCours(v, 12_000);
        assertThrows(BusinessException.class, () -> vehiculeService.retour(s.getId(), new RetourRequest(11_990, null, null)));
    }

    @Test
    void leRetourNePeutEtreSaisiQuUneFois() {
        Vehicule v = auGarage(12_000);
        SortieVehicule s = enCours(v, 12_000);
        s.setDateRetour(LocalDateTime.now());
        assertThrows(BusinessException.class, () -> vehiculeService.retour(s.getId(), new RetourRequest(12_100, null, null)));
    }

    @Test
    void lImmatriculationEstNormalisee() {
        assertEquals("DK 1234 AB", VehiculeService.normaliser("  dk  1234 ab "));
    }
}
