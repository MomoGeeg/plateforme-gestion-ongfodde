package ong.fodde.modules.se.entity;

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
@Table(name = "champs_formulaire")
@Getter
@Setter
@NoArgsConstructor
public class ChampFormulaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "formulaire_id", nullable = false, columnDefinition = "uuid")
    private UUID formulaireId;

    @Column(name = "libelle", length = 300, nullable = false)
    private String libelle;

    @Column(name = "type_champ", length = 50, nullable = false)
    private String typeChamp;

    @Column(name = "est_obligatoire", nullable = false)
    private Boolean estObligatoire = true;

    @Column(name = "ordre", nullable = false)
    private Integer ordre = 1;

    @Column(name = "options", columnDefinition = "text")
    private String options;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
