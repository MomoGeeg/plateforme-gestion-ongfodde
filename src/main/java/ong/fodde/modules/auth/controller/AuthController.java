package ong.fodde.modules.auth.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ong.fodde.modules.auth.dto.InitAdminRequest;
import ong.fodde.modules.auth.dto.LoginRequest;
import ong.fodde.modules.auth.dto.LoginResponse;
import ong.fodde.modules.auth.dto.UserListResponse;
import ong.fodde.modules.auth.entity.User;
import ong.fodde.modules.auth.service.AuthService;
import ong.fodde.modules.auth.service.JournalConnexions;
import ong.fodde.modules.auth.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JournalConnexions journal;

    public AuthController(AuthService authService, UserService userService, JournalConnexions journal) {
        this.authService = authService;
        this.userService = userService;
        this.journal = journal;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest http) {
        LoginResponse response = authService.login(request, adresseIp(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(response);
    }

    /** Note l'heure de déconnexion dans l'historique des connexions. */
    @PostMapping("/deconnexion")
    public ResponseEntity<Void> deconnexion(@RequestBody(required = false) Map<String, String> body,
                                            @AuthenticationPrincipal User currentUser) {
        String id = body == null ? null : body.get("connexionId");
        if (id != null && currentUser != null) {
            try {
                journal.deconnecter(UUID.fromString(id), currentUser.getId());
            } catch (IllegalArgumentException e) {
                // identifiant mal formé : rien à fermer
            }
        }
        return ResponseEntity.noContent().build();
    }

    /** Render place l'adresse du visiteur en tête de X-Forwarded-For. */
    static String adresseIp(HttpServletRequest http) {
        String transmise = http.getHeader("X-Forwarded-For");
        if (transmise != null && !transmise.isBlank()) {
            return transmise.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    @PostMapping("/init")
    public ResponseEntity<UserListResponse> initAdmin(@Valid @RequestBody InitAdminRequest request) {
        UserListResponse admin = userService.initAdmin(
                request.nom(), request.prenom(), request.email(), request.motDePasse());
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }
}
