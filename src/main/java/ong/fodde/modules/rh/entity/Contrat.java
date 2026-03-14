package ong.fodde.modules.rh.entity;

import java.math.BigDecimal;
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
import ong.fodde.modules.projets.entity.Devise;
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "contrats")
@Getter
@Setter
@NoArgsConstructor
public class Contrat extends BaseEntity {

    @Column(name = "employe_id", nullable = false, columnDefinition = "uuid")
    private UUID employeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, columnDefinition = "type_contrat")
    private TypeContrat typeContrat;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "salaire_base", nullable = false)
    private BigDecimal salaireBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "devise", columnDefinition = "devise")
    private Devise devise;

    @Column(name = "poste", length = 200, nullable = false)
    private String poste;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_contrat")
    private StatutContrat statut = StatutContrat.ACTIF;

    @Column(name = "document_url", columnDefinition = "text")
    private String documentUrl;
}
