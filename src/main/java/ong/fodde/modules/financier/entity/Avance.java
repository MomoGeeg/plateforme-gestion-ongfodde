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

@Entity
@Table(name = "avances")
@Getter
@Setter
@NoArgsConstructor
public class Avance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "beneficiaire_id", nullable = false, columnDefinition = "uuid")
    private UUID beneficiaireId;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    @Column(name = "motif", nullable = false, columnDefinition = "text")
    private String motif;

    @Column(name = "date_octroi", nullable = false)
    private LocalDate dateOctroi;

    @Column(name = "date_justification")
    private LocalDate dateJustification;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_avance")
    private StatutAvance statut = StatutAvance.EN_COURS;

    @Column(name = "montant_justifie")
    private BigDecimal montantJustifie = BigDecimal.ZERO;

    @Column(name = "created_at")
private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;
}
