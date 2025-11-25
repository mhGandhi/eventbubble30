package com.lennadi.eventbubble30.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Collection;

@Entity
@Getter
@Setter
public class Benutzer {
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, updatable = false)
    private Long id;

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

    @Column(nullable = false)//todo enum/set oder so
    private String role = ROLE_USER;

    @Column(nullable = false, updatable = false)
    private Instant creationDate;
    @Column(nullable = false)
    private Instant modificationDate;
    private Instant lastLoginDate;
    private Instant lastSeen;

    @Column(nullable = false)
    private Instant passwordChangedAt = Instant.EPOCH;

    @Column(nullable = false)
    private Instant tokensInvalidatedAt = Instant.EPOCH;


    public boolean hasRole(String role) {
        return this.role!=null&&this.role.equals(role);
    }

    public DTO toDTO() {
        return new DTO(this.getId(), this.getUsername());
    }

    public static record DTO(
            Long id,
            String username
    ) { }
}
