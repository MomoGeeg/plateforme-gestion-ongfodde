package ong.fodde.modules.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/init")
    public ResponseEntity<UserListResponse> initAdmin(@Valid @RequestBody InitAdminRequest request) {
        UserListResponse admin = userService.initAdmin(
                request.nom(), request.prenom(), request.email(), request.motDePasse());
        return ResponseEntity.status(HttpStatus.CREATED).body(admin);
    }
}
