package com.lennadi.eventbubble30.features.db.entities;

import com.lennadi.eventbubble30.features.db.EntityType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@Table(indexes = {
        @Index(name = "idx_user_external_id", columnList = "external_id"),
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_verification_token", columnList = "verification_token")
})
public class Benutzer extends BaseEntity{
    public static final EntityType TYPE = EntityType.USER;

    @Column(unique = true, nullable = false, length = 20)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "besitzer")
    private Collection<Veranstaltung> veranstaltungen;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "benutzer_roles",
            joinColumns = @JoinColumn(name = "benutzer_id")
    )
    @Column(nullable = false)
    private Set<Role> roles = new HashSet<>(List.of(Role.USER));

    private Instant lastLoginDate;
    private Instant lastSeen;

    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant passwordChangedAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant tokensInvalidatedAt = Instant.EPOCH;

    @Column(nullable = false)
    private boolean emailVerified = false;
    private String verificationToken;
    private Instant verificationTokenExpiresAt;

    @ManyToMany
    @JoinTable(
            name = "benutzer_bookmarked_veranstaltungen",
            joinColumns = @JoinColumn(name="benutzer_id"),
            inverseJoinColumns = @JoinColumn(name="veranstaltung_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_user_event_bookmark",
                    columnNames = {"benutzer_id", "veranstaltung_id"}
            )
    )
    private Set<Veranstaltung> bookmarkedVeranstaltungen = new HashSet<>();


    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public void setPasswordHash(String pwHash){
        if(!Objects.equals(pwHash, this.passwordHash)){
            this.passwordHash = pwHash;
            passwordChangedAt = Instant.now();
        }
    }

    public DTO toDTO() {
        return new DTO(this.getExternalId(), this.username, this.roles);
    }

    @Override
    public EntityType getType() {
        return TYPE;
    }

    public record DTO(
            String id,
            String username,
            Set<Role> roles
    ) { }

    public LargeDTO toLargeDTO() {
        return new LargeDTO(
                this.getExternalId(), this.getUsername(),
                this.getEmail(), this.getRoles(),
                this.getLastLoginDate(), this.getLastSeen(), this.getPasswordChangedAt(), this.getTokensInvalidatedAt(),
                this.isEmailVerified()
        );
    }

    public record LargeDTO(
            String id,
            String username,
            String email,
            Set<Role> roles,
            Instant lastLoginDate,
            Instant lastSeen,
            Instant passwordChangedAt,
            Instant tokensInvalidatedAt,
            boolean emailVerified
    ) { }

    public enum Role{
        USER,
        ADMIN,
        MODERATOR
    }
}
