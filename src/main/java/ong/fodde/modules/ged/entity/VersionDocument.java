package ong.fodde.modules.ged.entity;

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
@Table(name = "versions_document")
@Getter
@Setter
@NoArgsConstructor
public class VersionDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "document_id", nullable = false, columnDefinition = "uuid")
    private UUID documentId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "taille")
    private Long taille;

    @Column(name = "type_fichier")
    private String typeFichier;

    @Column(name = "uploade_par", nullable = false, columnDefinition = "uuid")
    private UUID uploadePar;

    @Column(name = "uploade_le")
    private LocalDateTime uploadeLe = LocalDateTime.now();
}
