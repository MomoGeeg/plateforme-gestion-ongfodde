package ong.fodde.modules.rh.dto;

import java.util.List;
import java.util.UUID;

import ong.fodde.modules.auth.entity.StatutUser;

/**
 * Compte de la plateforme qui n'a pas encore de fiche employé. Les RH s'en
 * servent pour créer la fiche déjà liée au compte (nom, prénom, téléphone
 * et service préremplis). Pas de donnée sensible : ni mot de passe, ni connexions.
 */
public record CompteSansFicheResponse(
    UUID id,
    String nom,
    String prenom,
    String email,
    String telephone,
    String service,
    StatutUser statut,
    List<String> roles
) {}
