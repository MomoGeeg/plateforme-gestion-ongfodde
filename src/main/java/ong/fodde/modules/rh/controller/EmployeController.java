package ong.fodde.modules.rh.controller;

import java.util.List;
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
import ong.fodde.modules.rh.dto.EmployeRequest;
import ong.fodde.modules.rh.dto.EmployeResponse;
import ong.fodde.modules.rh.entity.StatutEmploye;
import ong.fodde.modules.rh.service.EmployeService;

@RestController
@RequestMapping("/api/employes")
public class EmployeController {

    private final EmployeService employeService;

    public EmployeController(EmployeService employeService) {
        this.employeService = employeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeResponse> creerEmploye(@Valid @RequestBody EmployeRequest request) {
        EmployeResponse response = employeService.creerEmploye(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<EmployeResponse>> listerEmployes() {
        return ResponseEntity.ok(employeService.listerEmployes());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeResponse> obtenirEmploye(@PathVariable UUID id) {
        return ResponseEntity.ok(employeService.obtenirEmploye(id));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('ROLE_RH') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<EmployeResponse> changerStatut(@PathVariable UUID id,
                                                          @RequestParam StatutEmploye statut) {
        return ResponseEntity.ok(employeService.changerStatut(id, statut));
    }
}
