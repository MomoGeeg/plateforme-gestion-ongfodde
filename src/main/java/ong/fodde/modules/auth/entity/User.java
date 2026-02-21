package ong.fodde.modules.auth.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "mot_de_passe_hash", nullable = false)
    private String motDePasseHash;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "service")
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutUser statut = StatutUser.EN_ATTENTE;

    @Column(name = "tentatives_echouees", nullable = false)
    private Integer tentativesEchouees = 0;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
