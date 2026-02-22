package ong.fodde.modules.financier.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.modules.projets.entity.Devise;

@Entity
@Table(name = "financements")
@Getter
@Setter
@NoArgsConstructor
public class Financement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "bailleur_id", nullable = false, columnDefinition = "uuid")
    private UUID bailleurId;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "reference_convention", length = 100, nullable = false)
    private String referenceConvention;

    @Column(name = "montant_accorde", nullable = false)
    private BigDecimal montantAccorde;

    @Enumerated(EnumType.STRING)
    @Column(name = "devise", nullable = false, columnDefinition = "devise")
    private Devise devise;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "conditions_reporting", columnDefinition = "text")
    private String conditionsReporting;

    @Column(name = "created_at")
private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;
}
