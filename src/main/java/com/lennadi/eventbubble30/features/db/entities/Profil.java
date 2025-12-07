package com.lennadi.eventbubble30.features.db.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
public class Profil {

    @Id @Column(unique = true, nullable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant creationDate;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant modificationDate;

    @PrePersist
    protected void onCreate() {
        this.creationDate = Instant.now();
        this.modificationDate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = Instant.now();
    }

    @OneToOne @MapsId @JoinColumn(name = "id", unique = true, nullable = false)
    private Benutzer benutzer;

    private String name;
    private String bio;

    public DTO toDTO(){
        return new DTO(id, getName(), getBio());
    }
    public record DTO(long id, String name, String bio) {}
}
