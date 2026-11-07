package ong.fodde.modules.rh.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.rh.dto.EvaluationRequest;
import ong.fodde.modules.rh.dto.EvaluationResponse;
import ong.fodde.modules.rh.service.EvaluationService;
import ong.fodde.shared.securite.AccesPersonnel;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final AccesPersonnel accesPersonnel;

    public EvaluationController(EvaluationService evaluationService, AccesPersonnel accesPersonnel) {
        this.evaluationService = evaluationService;
        this.accesPersonnel = accesPersonnel;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EvaluationResponse> creerEvaluation(@Valid @RequestBody EvaluationRequest request) {
        EvaluationResponse response = evaluationService.creerEvaluation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EvaluationResponse>> listerTous() {
        return ResponseEntity.ok(evaluationService.listerTous());
    }

    @GetMapping("/employe/{employeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EvaluationResponse>> listerEvaluationsEmploye(@PathVariable UUID employeId,
                                                                             @AuthenticationPrincipal User currentUser) {
        accesPersonnel.verifierEmploye(employeId, currentUser,
                "ROLE_RH", "ROLE_ADMIN", "ROLE_SE", "ROLE_RESPONSABLE_PROGRAMMES");
        return ResponseEntity.ok(evaluationService.listerEvaluationsEmploye(employeId));
    }

    @PatchMapping("/{id}/soumettre")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EvaluationResponse> soumettreEvaluation(@PathVariable UUID id) {
        return ResponseEntity.ok(evaluationService.soumettreEvaluation(id));
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EvaluationResponse> validerEvaluation(@PathVariable UUID id) {
        return ResponseEntity.ok(evaluationService.validerEvaluation(id));
    }

    @PatchMapping("/{id}/lire")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EvaluationResponse> confirmerLecture(@PathVariable UUID id) {
        return ResponseEntity.ok(evaluationService.confirmerLecture(id));
    }
}
