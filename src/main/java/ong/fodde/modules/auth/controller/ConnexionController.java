package ong.fodde.modules.auth.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ong.fodde.modules.auth.dto.ConnexionResponse;
import ong.fodde.modules.auth.service.JournalConnexions;

/** Historique des connexions, réservé à l'administrateur. */
@RestController
@RequestMapping("/api/admin/connexions")
public class ConnexionController {

    private final JournalConnexions journal;

    public ConnexionController(JournalConnexions journal) {
        this.journal = journal;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ConnexionResponse>> lister(
            @RequestParam(required = false) UUID userId,
            @RequestParam(defaultValue = "30") int jours) {
        return ResponseEntity.ok(journal.lister(userId, jours));
    }
}
