package ong.fodde.modules.financier.entity;

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
@Table(name = "pieces_justificatives")
@Getter
@Setter
@NoArgsConstructor
public class PieceJustificative {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "depense_id", nullable = false, columnDefinition = "uuid")
    private UUID depenseId;

    @Column(name = "nom", length = 255, nullable = false)
    private String nom;

    @Column(name = "url", nullable = false, columnDefinition = "text")
    private String url;

    @Column(name = "type_fichier", length = 20)
    private String typeFichier;

    @Column(name = "taille")
    private Long taille;

    @Column(name = "created_at")
private LocalDateTime createdAt = LocalDateTime.now();
}
