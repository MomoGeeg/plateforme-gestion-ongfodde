package ong.fodde.modules.auth.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import ong.fodde.modules.auth.entity.StatutUser;

public record UpdateUserRequest(
    @NotBlank(message = "Le nom est obligatoire")
    String nom,

    @NotBlank(message = "Le prénom est obligatoire")
    String prenom,

    String telephone,

    String service,

    @NotEmpty(message = "Au moins un rôle est obligatoire")
    List<String> roles,

    StatutUser statut
) {}
