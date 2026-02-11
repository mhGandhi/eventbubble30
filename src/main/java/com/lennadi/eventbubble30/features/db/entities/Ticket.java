package com.lennadi.eventbubble30.features.db.entities;

import com.lennadi.eventbubble30.features.db.EntityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
public class Ticket{
    public static final EntityType TYPE = EntityType.TICKET;

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /// ///////////////////////////////////////////////////////////////////////////userspace
    @Column(name = "creation_date", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant creationDate;


    /// ///////////////////////////////////////////////////////////////////////////workspace
    @Column(name = "modification_date", nullable = false)
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

    public EntityType getType() {return TYPE;}
}
