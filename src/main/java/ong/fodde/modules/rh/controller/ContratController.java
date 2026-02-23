package ong.fodde.modules.rh.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ong.fodde.modules.rh.dto.ContratRequest;
import ong.fodde.modules.rh.dto.ContratResponse;
import ong.fodde.modules.rh.service.ContratService;

@RestController
@RequestMapping("/api/contrats")
public class ContratController {

    private final ContratService contratService;

    public ContratController(ContratService contratService) {
        this.contratService = contratService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ContratResponse> creerContrat(@Valid @RequestBody ContratRequest request) {
        ContratResponse response = contratService.creerContrat(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/employe/{employeId}")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ContratResponse>> listerContrats(@PathVariable UUID employeId) {
        return ResponseEntity.ok(contratService.listerContratsEmploye(employeId));
    }

    @GetMapping("/employe/{employeId}/actif")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContratResponse> contratActif(@PathVariable UUID employeId) {
        return ResponseEntity.ok(contratService.contratActif(employeId));
    }
}
