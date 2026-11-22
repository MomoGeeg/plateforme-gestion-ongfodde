package ong.fodde.modules.projets.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ong.fodde.shared.entity.BaseEntity;

/** Animateur rattaché à un projet ; le coordonnateur du projet valide son planning et ses rapports. */
@Entity
@Table(name = "animateurs_projet")
@Getter
@Setter
@NoArgsConstructor
public class AnimateurProjet extends BaseEntity {

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "animateur_id", nullable = false, columnDefinition = "uuid")
    private UUID animateurId;
}
