package com.lennadi.eventbubble30.features.db.entities.tickets;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Inheritance(strategy = InheritanceType.JOINED)
@Setter @Getter
@Entity
@Table(
        name = "ticket",
        indexes = {
                @Index(name = "idx_creation_date", columnList = "creation_date"),
                @Index(name = "idx_modification_date", columnList = "modification_date"),
                @Index(name = "idx_closed", columnList = "closed"),
                @Index(name = "idx_escalate", columnList = "escalate"),
                @Index(name = "idx_created_by", columnList = "created_by"),
                @Index(name = "idx_assigned_to", columnList = "assigned_to"),
        }
)
public class Ticket{
    public static final EntityType TYPE = EntityType.TICKET;


    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name="external_id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private String externalId;

    /// ///////////////////////////////////////////////////////////////////////////userspace
    @Column(name = "creation_date", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant creationDate;

    @Column(name="message", updatable = false)
    @Setter(AccessLevel.NONE)
    private String message;

    @JoinColumn(name = "created_by", updatable = false)
    @Setter(AccessLevel.NONE)
    @ManyToOne
    private Benutzer createdBy;


    /// ///////////////////////////////////////////////////////////////////////////workspace
    @Column(name = "modification_date", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant modificationDate;

    @Column(name="closed", nullable = false)
    private boolean closed = false;

    @Column(name="escalate", nullable = false)
    private boolean escalate = false;

    @Column(name="comment")
    private String comment;

    @JoinColumn(name="assigned_to")
    @ManyToOne
    private Benutzer assignedTo;



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
