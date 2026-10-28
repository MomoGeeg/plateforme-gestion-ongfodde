package ong.fodde.modules.rapports.entity;

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
@Table(name = "rapports")
@Getter
@Setter
@NoArgsConstructor
public class Rapport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TypeRapport type;

    @Column(name = "type_label", length = 200)
    private String typeLabel;

    @Column(name = "periode", length = 100)
    private String periode;

    @Column(name = "projet_code", length = 50)
    private String projetCode;

    @Column(name = "bailleur", length = 100)
    private String bailleur;

    @Column(name = "format", length = 10)
    private String format;

    @Column(name = "archive", nullable = false)
    private Boolean archive = false;

    @Column(name = "genere_par_id", columnDefinition = "uuid")
    private UUID genereParId;

    @Column(name = "genere_par", length = 200)
    private String generePar;

    @Column(name = "date_generation")
    private LocalDateTime dateGeneration = LocalDateTime.now();
}
