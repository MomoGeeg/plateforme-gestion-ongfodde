package ong.fodde.modules.rh.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "employes")
@Getter
@Setter
@NoArgsConstructor
public class Employe extends BaseEntity {

    @Column(name = "matricule", length = 20, nullable = false, unique = true)
    private String matricule;

    @Column(name = "user_id", columnDefinition = "uuid", unique = true)
    private UUID userId;

    @Column(name = "nom", length = 100, nullable = false)
    private String nom;

    @Column(name = "prenom", length = 100, nullable = false)
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance", length = 200)
    private String lieuNaissance;

    @Column(name = "nationalite", length = 100)
    private String nationalite;

    @Column(name = "telephone", length = 20)
    private String telephone;

    @Column(name = "adresse", columnDefinition = "text")
    private String adresse;

    @Column(name = "poste", length = 200, nullable = false)
    private String poste;

    @Column(name = "departement", length = 100)
    private String departement;

    @Column(name = "date_embauche", nullable = false)
    private LocalDate dateEmbauche;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_employe")
    private StatutEmploye statut = StatutEmploye.ACTIF;

    @Column(name = "superviseur_id", columnDefinition = "uuid")
    private UUID superviseurId;
}
