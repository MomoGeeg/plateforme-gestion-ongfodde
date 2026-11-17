package ong.fodde.modules.auth.controller;

import org.springframework.http.HttpStatus;
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
import ong.fodde.modules.auth.service.AuthService;
import ong.fodde.modules.auth.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest http) {
        LoginResponse response = authService.login(request, adresseIp(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(response);
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
