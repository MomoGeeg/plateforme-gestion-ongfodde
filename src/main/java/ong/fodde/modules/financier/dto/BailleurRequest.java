package ong.fodde.modules.financier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BailleurRequest(
    @NotBlank(message = "Le nom du bailleur est obligatoire")
    @Size(max = 200)
    String nom,
    @Size(max = 20)
    String sigle,
    @Size(max = 100)
    String pays,
    @Size(max = 200)
    String contact,
    @Email(message = "Adresse e-mail invalide")
    @Size(max = 255)
    String email
) {}
