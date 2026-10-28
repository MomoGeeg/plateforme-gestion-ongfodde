package ong.fodde.modules.auth.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;

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
import ong.fodde.modules.auth.dto.CreateUserRequest;
import ong.fodde.modules.auth.dto.UpdateUserRequest;
import ong.fodde.modules.auth.dto.UserListResponse;
import ong.fodde.modules.auth.entity.StatutUser;
import ong.fodde.modules.auth.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserListResponse>> listerUtilisateurs() {
        return ResponseEntity.ok(userService.listerUtilisateurs());
    }

    @GetMapping("/actifs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserListResponse>> listerUsersActifs() {
        return ResponseEntity.ok(userService.listerUsersActifs());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserListResponse> creerUtilisateur(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.creerUtilisateur(request));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserListResponse> changerStatut(@PathVariable UUID id,
                                                           @RequestParam StatutUser statut) {
        return ResponseEntity.ok(userService.changerStatut(id, statut));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserListResponse> obtenirUtilisateur(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.obtenirUtilisateur(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserListResponse> mettreAJourUtilisateur(@PathVariable UUID id,
                                                                    @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.mettreAJourUtilisateur(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> supprimerUtilisateur(@PathVariable UUID id) {
        userService.supprimerUtilisateur(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reinitialiser-motdepasse")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserListResponse> reinitialiserMotDePasse(@PathVariable UUID id,
                                                                     @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.reinitialiserMotDePasse(id, body.get("motDePasse")));
    }

    @PatchMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserListResponse> mettreAJourRoles(@PathVariable UUID id,
                                                              @RequestBody List<String> roles) {
        return ResponseEntity.ok(userService.mettreAJourRoles(id, roles));
    }
}
