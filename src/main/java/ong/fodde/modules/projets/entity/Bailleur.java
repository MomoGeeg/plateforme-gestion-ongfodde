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
@Table(name = "bailleurs")
@Getter
@Setter
@NoArgsConstructor
public class Bailleur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "nom", length = 200, nullable = false, unique = true)
    private String nom;

    @Column(name = "sigle", length = 20)
    private String sigle;

    @Column(name = "pays", length = 100)
    private String pays;

    @Column(name = "contact", length = 200)
    private String contact;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
