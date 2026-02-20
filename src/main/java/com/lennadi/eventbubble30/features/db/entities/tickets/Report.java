package com.lennadi.eventbubble30.features.db.entities.tickets;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(
        name = "report",
        indexes = {
                @Index(name = "idx_resource_id", columnList = "resource_id"),
                @Index(name = "idx_entity_type", columnList = "entity_type"),
        }
)
@Getter @Setter
@DiscriminatorValue("REPORT")
public class Report extends Ticket{
    /// ///////////////////////////////////////////////////////////////////////////userspace
    @Column(name="entity_type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private EntityType entityType;

    @Column(name="resource_id", updatable = false)
    @Setter(AccessLevel.NONE)
    private String resourceId; //UUID oder Long als String gespeichert

    @Column(name="reason", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Reason reason = Reason.OTHER;

    @Column(name="reason_text", updatable = false)
    @Setter(AccessLevel.NONE)
    private String reason_text; //if Other

    /// ///////////////////////////////////////////////////////////////////////////workspace
    @Column(name="outcome")
    private Outcome outcome;

    public Report(String message,
                  Benutzer createdBy,
                  EntityType entityType,
                  String resourceId,
                  Reason reason,
                  String reasonText) {

        super(message, createdBy);

        this.entityType = entityType;
        this.resourceId = resourceId;
        this.reason = reason == null ? Reason.OTHER : reason;
        this.reason_text = reasonText;
    }

    protected Report() {

    }

    @Transient
    @Override
    public Map<String, Object> getDetails(){
        return Map.of(
                "entityType", entityType,
                "resourceId", resourceId,
                "reason", reason,
                "reasonText", reason_text,
                "outcome", outcome
        );
    }
    public enum Reason {OTHER, SPAM, INAPPROPRIATE, HATE_SPEECH, CYBER_BULLYING, MISINFORMATION}
    public enum Outcome { DELETED, CREATOR_BANNED, NOTHING, STRAIGHT_UP_CALLED_THE_POPO_ON_THIS_FREAK}
}
