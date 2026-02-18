package com.lennadi.eventbubble30.logging;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter @Setter(AccessLevel.NONE)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, updatable = false)
    private Long id;

    //WHO
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "benutzer_id", nullable = true, updatable = false)
    private Benutzer benutzer;

    @Column(nullable = true, updatable = false)
    private String usernameSnapshot;

    @Column(nullable = false, updatable = false, length = 45)
    private String ipAddress;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "auditlog_role_snapshots",
            joinColumns = @JoinColumn(name = "auditlog_id")
    )
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Benutzer.Role> roleSnapshot;

    //WHAT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private Action action;

    @Column(nullable = false, updatable = false, length = 8000)
    private String payload;

    @Column(nullable = false, updatable = false)
    private boolean success;

    @Column(nullable = false, updatable = false, length = 255)
    private String endpoint;

    //WHEN
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    //ON WHAT
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private EntityType resourceType;
    @Column(nullable = true, updatable = false)
    private String resourceId;

    public AuditLog(
            Benutzer benutzer,
            String ipAddress,
            String usernameSnapshot,
            Set<Benutzer.Role> roleSnapshot,
            Action action,
            String payload,
            boolean success,
            String endpoint,
            Instant timestamp,
            EntityType resourceType,
            String resourceId
    ) {
        this.benutzer = benutzer;
        this.ipAddress = ipAddress;
        this.usernameSnapshot = usernameSnapshot;
        this.roleSnapshot = roleSnapshot;
        this.action = action;
        this.payload = payload;
        this.success = success;
        this.endpoint = endpoint;
        this.timestamp = timestamp;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    protected AuditLog() {

    }

    public enum Action {
        //CRUD
        CREATE, READ, UPDATE, DELETE,
        //User
        SIGNUP, LOGIN, REFRESH, INVALIDATE_TOKENS, MAIL_REQUEST,
        OTHER, CLEANUP_UNVERIFIED_ACCOUNTS;

        public static final Set<Action> CRUD = new HashSet<>(List.of(CREATE, READ, UPDATE, DELETE));
        public static final Set<Action> AUTH = new HashSet<>(List.of(SIGNUP, LOGIN, REFRESH, INVALIDATE_TOKENS, MAIL_REQUEST));
    }

    public DTO toDTO(){
        return new DTO(
                this.id, this.benutzer!=null?this.benutzer.toDTO():null, this.ipAddress, this.usernameSnapshot, this.roleSnapshot,
                this.action, this.payload, this.success, this.endpoint,
                this.timestamp,
                this.resourceType, this.resourceId
        );
    }
    public record DTO (
            Long id, Benutzer.DTO user, String ipAddress, String usernameSnapshot, Set<Benutzer.Role> roleSnapshot,
            Action action, String payload, boolean success, String endpoint,
            Instant timestamp,
            EntityType resourceType, String resourceId
    ) { }
}
