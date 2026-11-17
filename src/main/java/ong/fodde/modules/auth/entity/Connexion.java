package ong.fodde.modules.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une tentative de connexion, réussie ou refusée. Une ligne n'est jamais modifiée.
 * Pas de lien JPA vers User : l'historique survit à la suppression du compte.
 */
@Entity
@Table(name = "historique_connexions")
@Getter
@Setter
@NoArgsConstructor
public class Connexion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", columnDefinition = "uuid", updatable = false)
    private UUID userId;

    @Column(name = "email", nullable = false, length = 255, updatable = false)
    private String email;

    @Column(name = "nom_complet", length = 200, updatable = false)
    private String nomComplet;

    @Column(name = "succes", nullable = false, updatable = false)
    private boolean succes;

    @Column(name = "motif_echec", length = 30, updatable = false)
    private String motifEchec;

    @Column(name = "adresse_ip", length = 64, updatable = false)
    private String adresseIp;

    @Column(name = "navigateur", length = 300, updatable = false)
    private String navigateur;

    @Column(name = "date_connexion", nullable = false, updatable = false)
    private LocalDateTime dateConnexion;
}
