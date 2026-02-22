package ong.fodde.modules.projets.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resultats_attendus")
@Getter
@Setter
@NoArgsConstructor
public class ResultatAttendu {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "projet_id", nullable = false, columnDefinition = "uuid")
    private UUID projetId;

    @Column(name = "libelle", nullable = false, columnDefinition = "text")
    private String libelle;

    @Column(name = "ordre")
    private Integer ordre = 1;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
