package com.lennadi.eventbubble30.features.db.entities.tickets;

import com.lennadi.eventbubble30.features.db.EntityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "report",
        indexes = {
                @Index(name = "idx_resource_id", columnList = "resource_id"),
                @Index(name = "idx_entity_type", columnList = "entity_type"),
        }
)
@Getter @Setter
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


    public enum Reason {OTHER, SPAM, INAPPROPRIATE, HATE_SPEECH, CYBER_BULLYING, MISINFORMATION}

    public enum Outcome { DELETED, CREATOR_BANNED, NOTHING, STRAIGHT_UP_CALLED_THE_POPO_ON_THIS_FREAK}

    public record ReportDTO(
            long id,
            Instant creationDate,
            Instant modificationDate,
            String message,
            Long createdById,
            boolean closed,
            boolean escalate,
            String comment,
            Long assignedToId,
            EntityType entityType,
            String resourceId,
            Reason reason,
            String reasonText,
            Outcome outcome
    ){};

    public ReportDTO toDTO(){
        return new ReportDTO(
                this.getId(),
                this.getCreationDate(),
                this.getModificationDate(),
                this.getMessage(),
                this.getCreatedBy()==null?null:this.getCreatedBy().getId(),
                this.isClosed(),
                this.isEscalate(),
                this.getComment(),
                this.getAssignedTo()==null?null:this.getAssignedTo().getId(),
                this.getEntityType(),
                this.getResourceId(),
                this.getReason(),
                this.getReason_text(),
                this.getOutcome()
        );
    }
}
