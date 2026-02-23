package ong.fodde.modules.se.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.se.dto.DonneeCollecteeRequest;
import ong.fodde.modules.se.dto.DonneeCollecteeResponse;
import ong.fodde.modules.se.dto.FormulaireRequest;
import ong.fodde.modules.se.dto.FormulaireResponse;
import ong.fodde.modules.se.service.FormulaireService;

@RestController
@RequestMapping("/api/formulaires")
public class FormulaireController {

    private final FormulaireService formulaireService;

    public FormulaireController(FormulaireService formulaireService) {
        this.formulaireService = formulaireService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FormulaireResponse> creerFormulaire(@Valid @RequestBody FormulaireRequest request) {
        FormulaireResponse response = formulaireService.creerFormulaire(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/publier")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<FormulaireResponse> publierFormulaire(@PathVariable UUID id) {
        return ResponseEntity.ok(formulaireService.publierFormulaire(id));
    }

    @GetMapping("/projet/{projetId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FormulaireResponse>> listerFormulaires(@PathVariable UUID projetId) {
        return ResponseEntity.ok(formulaireService.listerFormulaires(projetId));
    }

    @PostMapping("/{id}/reponses")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonneeCollecteeResponse> soumettreReponse(
            @PathVariable UUID id,
            @Valid @RequestBody DonneeCollecteeRequest request,
            @RequestParam UUID collecteurId) {
        DonneeCollecteeRequest requestAvecFormulaire = new DonneeCollecteeRequest(
            id, request.dateCollecte(), request.localisation(), request.donnees()
        );
        DonneeCollecteeResponse response = formulaireService.soumettreReponse(requestAvecFormulaire, collecteurId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/reponses/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DonneeCollecteeResponse> validerReponse(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        UUID validateurId = body.get("validateurId");
        return ResponseEntity.ok(formulaireService.validerReponse(id, validateurId));
    }
}
