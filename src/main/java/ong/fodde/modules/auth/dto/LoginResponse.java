package ong.fodde.modules.auth.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
    String token,
    String type,
    UUID userId,
    String email,
    String nom,
    String prenom,
    List<String> roles
) {
    public LoginResponse(String token, UUID userId, String email, String nom, String prenom, List<String> roles) {
        this(token, "Bearer", userId, email, nom, prenom, roles);
    }
}
