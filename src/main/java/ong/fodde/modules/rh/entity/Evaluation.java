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
import ong.fodde.shared.entity.BaseEntity;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
public class Evaluation extends BaseEntity {

    @Column(name = "employe_id", nullable = false, columnDefinition = "uuid")
    private UUID employeId;

    @Column(name = "evaluateur_id", nullable = false, columnDefinition = "uuid")
    private UUID evaluateurId;

    @Column(name = "periode", length = 50, nullable = false)
    private String periode;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, columnDefinition = "statut_eval")
    private StatutEval statut = StatutEval.EN_COURS;

    @Column(name = "note_globale")
    private BigDecimal noteGlobale;

    @Column(name = "commentaires", columnDefinition = "text")
    private String commentaires;

    @Column(name = "points_forts", columnDefinition = "text")
    private String pointsForts;

    @Column(name = "axes_amelioration", columnDefinition = "text")
    private String axesAmelioration;

    @Column(name = "date_evaluation")
    private LocalDate dateEvaluation;
}
