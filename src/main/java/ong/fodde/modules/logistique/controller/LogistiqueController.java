package ong.fodde.modules.logistique.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.DepartRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MaterielRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MaterielResponse;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MouvementRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.MouvementResponse;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.PersonneResponse;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.RetourRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.SortieResponse;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.VehiculeRequest;
import ong.fodde.modules.logistique.dto.LogistiqueDtos.VehiculeResponse;
import ong.fodde.modules.logistique.service.MaterielService;
import ong.fodde.modules.logistique.service.VehiculeService;

/**
 * Logistique : inventaire du matériel, parc de véhicules, carnet de bord.
 * Lecture : responsable logistique, RAF, SE, admin. Saisie : responsable logistique, admin.
 */
@RestController
@RequestMapping("/api/logistique")
public class LogistiqueController {

    private static final String LECTURE =
        "hasAuthority('ROLE_RESPONSABLE_LOGISTIQUE') or hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')";
    private static final String SAISIE =
        "hasAuthority('ROLE_RESPONSABLE_LOGISTIQUE') or hasAuthority('ROLE_ADMIN')";

    private final MaterielService materielService;
    private final VehiculeService vehiculeService;

    public LogistiqueController(MaterielService materielService, VehiculeService vehiculeService) {
        this.materielService = materielService;
        this.vehiculeService = vehiculeService;
    }

    // ── Matériel ────────────────────────────────────────────────────────────

    @GetMapping("/materiels")
    @PreAuthorize(LECTURE)
    public List<MaterielResponse> listerMateriels() {
        return materielService.lister();
    }

    @GetMapping("/materiels/{id}")
    @PreAuthorize(LECTURE)
    public MaterielResponse obtenirMateriel(@PathVariable UUID id) {
        return materielService.obtenir(id);
    }

    @PostMapping("/materiels")
    @PreAuthorize(SAISIE)
    public ResponseEntity<MaterielResponse> creerMateriel(@Valid @RequestBody MaterielRequest req,
                                                          @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materielService.creer(req, currentUser.getId()));
    }

    @PutMapping("/materiels/{id}")
    @PreAuthorize(SAISIE)
    public MaterielResponse modifierMateriel(@PathVariable UUID id, @Valid @RequestBody MaterielRequest req) {
        return materielService.modifier(id, req);
    }

    @GetMapping("/materiels/{id}/mouvements")
    @PreAuthorize(LECTURE)
    public List<MouvementResponse> mouvements(@PathVariable UUID id) {
        return materielService.mouvements(id);
    }

    @PostMapping("/materiels/{id}/mouvements")
    @PreAuthorize(SAISIE)
    public ResponseEntity<MouvementResponse> enregistrerMouvement(@PathVariable UUID id,
                                                                  @Valid @RequestBody MouvementRequest req,
                                                                  @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materielService.enregistrerMouvement(id, req, currentUser.getId()));
    }

    // ── Véhicules ───────────────────────────────────────────────────────────

    @GetMapping("/vehicules")
    @PreAuthorize(LECTURE)
    public List<VehiculeResponse> listerVehicules() {
        return vehiculeService.lister();
    }

    @GetMapping("/vehicules/{id}")
    @PreAuthorize(LECTURE)
    public VehiculeResponse obtenirVehicule(@PathVariable UUID id) {
        return vehiculeService.obtenir(id);
    }

    @PostMapping("/vehicules")
    @PreAuthorize(SAISIE)
    public ResponseEntity<VehiculeResponse> creerVehicule(@Valid @RequestBody VehiculeRequest req,
                                                          @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeService.creer(req, currentUser.getId()));
    }

    @PutMapping("/vehicules/{id}")
    @PreAuthorize(SAISIE)
    public VehiculeResponse modifierVehicule(@PathVariable UUID id, @Valid @RequestBody VehiculeRequest req) {
        return vehiculeService.modifier(id, req);
    }

    // ── Carnet de bord ──────────────────────────────────────────────────────

    @GetMapping("/vehicules/{id}/sorties")
    @PreAuthorize(LECTURE)
    public List<SortieResponse> sorties(@PathVariable UUID id) {
        return vehiculeService.sorties(id);
    }

    @PostMapping("/vehicules/{id}/sorties")
    @PreAuthorize(SAISIE)
    public ResponseEntity<SortieResponse> depart(@PathVariable UUID id, @Valid @RequestBody DepartRequest req,
                                                 @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehiculeService.depart(id, req, currentUser.getId()));
    }

    @PatchMapping("/sorties/{id}/retour")
    @PreAuthorize(SAISIE)
    public SortieResponse retour(@PathVariable UUID id, @Valid @RequestBody RetourRequest req) {
        return vehiculeService.retour(id, req);
    }

    // ── Personnes ───────────────────────────────────────────────────────────

    /** Comptes actifs, pour choisir un responsable de bien ou un conducteur. */
    @GetMapping("/personnes")
    @PreAuthorize(LECTURE)
    public List<PersonneResponse> personnes() {
        return vehiculeService.personnes();
    }
}
