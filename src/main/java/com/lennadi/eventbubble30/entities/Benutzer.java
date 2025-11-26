package com.lennadi.eventbubble30.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
public class Benutzer extends BaseEntity{

    @Column(unique = true, nullable = false, length = 20)
    private String username;
    //todo erweitertes Profil (separate Tabelle? - Bio, Name, Avatar etc.)

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String email;
    //todo email verification (+removal job nach nh woche ohne)

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
    private Instant passwordChangedAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant tokensInvalidatedAt = Instant.EPOCH;


    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public DTO toDTO() {
        return new DTO(this.getId(), this.getUsername());
    }

    public static record DTO(
            Long id,
            String username
    ) { }

    public enum Role{
        USER,
        ADMIN,
        MODERATOR
    }
}
