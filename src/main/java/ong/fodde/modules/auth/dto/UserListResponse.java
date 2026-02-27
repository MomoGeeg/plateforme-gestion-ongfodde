package ong.fodde.modules.auth.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import ong.fodde.modules.auth.entity.StatutUser;

public record UserListResponse(
    UUID id,
    String nom,
    String prenom,
    String email,
    String telephone,
    String service,
    StatutUser statut,
    List<String> roles,
    LocalDateTime derniereConnexion,
    LocalDateTime createdAt
) {}
