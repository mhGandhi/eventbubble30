package com.lennadi.eventbubble30.features.db.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.time.Instant;

@Entity
@Getter
@Setter
public class Profil {

    @Id @Column(unique = true, nullable = false, updatable = false) @Setter(AccessLevel.NONE)
    private Long id;//immer gleich der BenutzerId

    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant creationDate;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant modificationDate;

    public Profil() {

    }

    @PrePersist
    protected void onCreate() {
        this.creationDate = Instant.now();
        this.modificationDate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = Instant.now();
    }


    @Column(nullable = false)
    private String name;
    private String bio;

    private String avatarKey;

    public Profil(long id, String pName){
        this.id = id;
        this.name = pName;
    }

    public record DTO(long id, String name, URL avatarURL, String bio) {}
    public record SmallDTO(long id, String name, URL avatarURL) {}
}
