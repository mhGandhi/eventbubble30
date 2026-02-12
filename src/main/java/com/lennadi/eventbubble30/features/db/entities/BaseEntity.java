package com.lennadi.eventbubble30.features.db.entities;

import com.lennadi.eventbubble30.features.db.EntityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "external_id", nullable = true, updatable = false, length=36)//todo non-nullable, non-updateable, unq
    @Setter(AccessLevel.NONE)
    private String externalId;

    @Column(name = "creation_date", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant creationDate;

    @Column(name = "modification_date", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant modificationDate;

    @Column(name = "deleted_at")
    private Instant deletedAt;//todo Logik

    @PrePersist
    protected void onCreate() {
        if (externalId == null) externalId = UUID.randomUUID().toString();
        this.creationDate = Instant.now();
        this.modificationDate = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = Instant.now();
    }


    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    public void restore() {
        this.deletedAt = null;
    }

    public abstract EntityType getType();
}