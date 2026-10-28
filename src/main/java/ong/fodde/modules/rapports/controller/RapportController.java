package ong.fodde.modules.rapports.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ong.fodde.modules.rapports.dto.RapportRequest;
import ong.fodde.modules.rapports.dto.RapportResponse;
import ong.fodde.modules.rapports.service.RapportService;

@RestController
@RequestMapping("/api/rapports")
public class RapportController {

    private final RapportService rapportService;

    public RapportController(RapportService rapportService) {
        this.rapportService = rapportService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<RapportResponse>> listerRapports() {
        return ResponseEntity.ok(rapportService.listerRapports());
    }

    @PostMapping("/generer")
    @PreAuthorize("hasAuthority('ROLE_RAF') or hasAuthority('ROLE_SE') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<RapportResponse> genererRapport(
            @RequestBody RapportRequest request,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                rapportService.genererRapport(request, userId));
    }
}
