package com.lennadi.eventbubble30.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter
public class Veranstaltung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant creationDate;
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant modificationDate;

    private Instant termin;

    private String title;
    private String description;

    @ManyToOne
    @JoinColumn(name = "besitzer_id")
    private Benutzer besitzer;

    @PrePersist
    protected void onCreate() {
        this.creationDate = Instant.now();
        this.modificationDate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = Instant.now();
    }

    public static record DTO(
            Long id,
            Instant creationDate,
            Instant modificationDate,
            Instant termin,
            String title,
            String description,

            Benutzer.DTO besitzer
    ) {}

    public DTO toDTO() {
        return new DTO(
                this.getId(),
                this.getCreationDate(),
                this.getModificationDate(),
                this.getTermin(),
                this.getTitle(),
                this.getDescription(),
                (this.getBesitzer()!=null?this.getBesitzer().toDTO():null)
        );
    }
}
