package ong.fodde.modules.acquisitions.entity;

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
@Table(name = "acquisitions")
@Getter
@Setter
@NoArgsConstructor
public class Acquisition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "code_actif", length = 50)
    private String codeActif;

    @Column(name = "designation", length = 300, nullable = false)
    private String designation;

    @Column(name = "justification", columnDefinition = "text")
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", length = 30)
    private CategorieAcquisition categorie;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "valeur", nullable = false)
    private BigDecimal valeur;

    @Column(name = "date_acquisition", nullable = false)
    private LocalDate dateAcquisition;

    @Column(name = "soumis_par_id", columnDefinition = "uuid")
    private UUID soumisParId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutAcquisition statut = StatutAcquisition.EN_ATTENTE;

    @Column(name = "motif_rejet", columnDefinition = "text")
    private String motifRejet;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
