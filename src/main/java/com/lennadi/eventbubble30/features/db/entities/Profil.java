package com.lennadi.eventbubble30.features.db.entities;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.fileStorage.templates.StoredFile;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(indexes = {
        @Index(name = "idx_external_id", columnList = "external_id")
})
public class Profil {//todo inheritance based mit inheritance type joined stattdessen (aufwändig lass mal nd machen)
    public static final EntityType TYPE = EntityType.PROFILE;

    @Id
    @Column(name="id", unique = true, nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;//immer gleich der BenutzerId

    @Column(name = "external_id", nullable = false, unique = true, updatable = false)
    @Setter(AccessLevel.NONE)
    private String externalId;


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

    @Embedded
    @AttributeOverride(
            name= "key",
            column = @Column(name="avatar_key")
    )
    private StoredFile avatar;

    public boolean hasAvatar(){
        return avatar != null;
    }

    public Profil(long id, String externalId, String pName){
        this.id = id;
        this.externalId = externalId;
        this.name = pName;
    }

    public record DTO(long id, String name, URL avatarURL, String bio) {}
    public record SmallDTO(long id, String name, URL avatarURL) {}

    public EntityType getType() {return TYPE;}
}
