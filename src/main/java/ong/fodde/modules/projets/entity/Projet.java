package ong.fodde.modules.projets.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import ong.fodde.modules.auth.entity.User;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "projets")
@Getter
@Setter
@NoArgsConstructor
public class Projet extends BaseEntity {

    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    @Column(name = "titre", length = 300, nullable = false)
    private String titre;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "objectif_general", columnDefinition = "text")
    private String objectifGeneral;

    @Column(name = "zone_geographique", length = 200)
    private String zoneGeographique;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "budget_total", nullable = false)
    private BigDecimal budgetTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "devise", nullable = false, columnDefinition = "devise")
    private Devise devise = Devise.XOF;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_projet")
    private StatutProjet statut = StatutProjet.BROUILLON;

    @Column(name = "responsable_id", nullable = false, columnDefinition = "uuid")
    private UUID responsableId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "projet_bailleurs",
        joinColumns = @JoinColumn(name = "projet_id", columnDefinition = "uuid"),
        inverseJoinColumns = @JoinColumn(name = "bailleur_id", columnDefinition = "uuid")
    )
    private Set<Bailleur> bailleurs = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "equipe_projet",
        joinColumns = @JoinColumn(name = "projet_id", columnDefinition = "uuid"),
        inverseJoinColumns = @JoinColumn(name = "user_id", columnDefinition = "uuid")
    )
    private Set<User> equipe = new HashSet<>();
}
