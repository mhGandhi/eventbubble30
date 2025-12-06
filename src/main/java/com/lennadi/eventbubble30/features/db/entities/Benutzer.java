package com.lennadi.eventbubble30.features.db.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
public class Benutzer extends BaseEntity{

    @Column(unique = true, nullable = false, length = 20)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String email;

    @OneToMany(mappedBy = "besitzer")
    private Collection<Veranstaltung> veranstaltungen;

    @OneToOne(mappedBy = "benutzer", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profil profil;

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
        return new DTO(this.getId(), this.getUsername());
    }

    public record DTO(
            Long id,
            String username
    ) { }

    public AdminDTO toAdminDTO() {
        return new AdminDTO(
                this.getId(), this.getUsername(),
                this.getEmail(), this.getRoles(),
                this.getLastLoginDate(), this.getLastSeen(), this.getPasswordChangedAt(), this.getTokensInvalidatedAt(),
                this.isEmailVerified());
    }

    public record AdminDTO(
            Long id,
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
