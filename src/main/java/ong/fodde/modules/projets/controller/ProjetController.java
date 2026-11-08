package ong.fodde.modules.projets.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.historique.dto.EtapeHistorique;
import ong.fodde.modules.projets.dto.MontageDtos.PersonneResponse;
import ong.fodde.modules.projets.dto.ProjetRequest;
import ong.fodde.modules.projets.dto.ProjetResponse;
import ong.fodde.modules.projets.entity.StatutProjet;
import ong.fodde.modules.projets.service.ProjetService;

/** Fiche projet, équipe et circuit de validation. L'auteur vient toujours du jeton. */
@RestController
@RequestMapping("/api/projets")
public class ProjetController {

    private final ProjetService projetService;

    public ProjetController(ProjetService projetService) {
        this.projetService = projetService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProjetResponse> creerProjet(@Valid @RequestBody ProjetRequest request,
                                                      @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetService.creerProjet(request, user));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ProjetResponse> listerProjets() {
        return projetService.listerProjets();
    }

    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()")
    public List<ProjetResponse> listerMesProjets(@AuthenticationPrincipal User user) {
        return projetService.listerMesProjets(user.getId());
    }

    /** Comptes actifs (nom et rôles) pour le montage : chef de projet, équipe, responsables d'activité. */
    @GetMapping("/personnes")
    @PreAuthorize("hasAuthority('ROLE_RESPONSABLE_PROGRAMMES') or hasAuthority('ROLE_CHARGE_PROGRAMME') or hasAuthority('ROLE_ADMIN')")
    public List<PersonneResponse> personnes() {
        return projetService.personnes();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProjetResponse obtenirProjet(@PathVariable UUID id) {
        return projetService.obtenirProjet(id);
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("isAuthenticated()")
    public List<EtapeHistorique> historique(@PathVariable UUID id) {
        return projetService.historique(id);
    }

    /** Fiche modifiable en brouillon par le RP, le chef de projet ou l'admin (vérifié par le service). */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ProjetResponse modifierProjet(@PathVariable UUID id, @Valid @RequestBody ProjetRequest request,
                                         @AuthenticationPrincipal User user) {
        return projetService.modifierProjet(id, request, user);
    }

    // ── Équipe (RP, chef de projet ou admin : vérifié par le service) ──

    @PostMapping("/{id}/equipe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> ajouterMembre(@PathVariable UUID id, @RequestBody Map<String, UUID> body,
                                              @AuthenticationPrincipal User user) {
        projetService.ajouterMembreEquipe(id, body.get("userId"), user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projetId}/equipe/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> retirerMembre(@PathVariable UUID projetId, @PathVariable UUID userId,
                                              @AuthenticationPrincipal User user) {
        projetService.retirerMembreEquipe(projetId, userId, user);
        return ResponseEntity.noContent().build();
    }

    // ── Circuit : RP → RAF → SE ──

    @PatchMapping("/{id}/soumettre")
    @PreAuthorize("isAuthenticated()")
    public ProjetResponse soumettre(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return projetService.soumettre(id, user);
    }

    @PatchMapping("/{id}/verifier-budget")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_ADMIN')")
    public ProjetResponse verifierBudget(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return projetService.verifierBudget(id, user);
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ProjetResponse valider(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return projetService.valider(id, user);
    }

    @PatchMapping("/{id}/renvoyer")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ProjetResponse renvoyer(@PathVariable UUID id, @RequestBody Map<String, String> body,
                                   @AuthenticationPrincipal User user) {
        return projetService.renvoyer(id, body.get("motif"), user);
    }

    /** Après l'activation : suspension, reprise, clôture (SE). */
    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ProjetResponse changerStatut(@PathVariable UUID id, @RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal User user) {
        return projetService.changerStatut(id, StatutProjet.valueOf(body.get("statut")), user,
                body.get("commentaire"));
    }

    @PatchMapping("/{id}/prolonger")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ProjetResponse prolongerDateFin(@PathVariable UUID id, @RequestParam LocalDate nouvelleDateFin,
                                           @RequestParam String justification) {
        return projetService.prolongerDateFin(id, nouvelleDateFin, justification);
    }
}
