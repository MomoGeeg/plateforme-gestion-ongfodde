package ong.fodde.modules.projets.entity;

import java.time.LocalDateTime;
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

/** Planning ou rapport du mois d'un technicien, validé par son superviseur. Un seul par type et par mois. */
@Entity
@Table(name = "suivis_mensuels")
@Getter
@Setter
@NoArgsConstructor
public class SuiviMensuel extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type_suivi", nullable = false, length = 20)
    private TypeSuivi type;

    @Column(name = "technicien_id", nullable = false, columnDefinition = "uuid")
    private UUID technicienId;

    /** Mois concerné, au format AAAA-MM. */
    @Column(name = "mois", nullable = false, length = 7)
    private String mois;

    @Column(name = "contenu", nullable = false, columnDefinition = "text")
    private String contenu;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutSuivi statut = StatutSuivi.SOUMIS;

    @Column(name = "commentaire", columnDefinition = "text")
    private String commentaire;

    @Column(name = "decide_par", columnDefinition = "uuid")
    private UUID decidePar;

    @Column(name = "date_decision")
    private LocalDateTime dateDecision;
}
