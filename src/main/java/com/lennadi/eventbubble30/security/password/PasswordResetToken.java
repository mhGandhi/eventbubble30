package com.lennadi.eventbubble30.security.password;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter @Setter(AccessLevel.NONE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false, updatable = false)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String token;

    @ManyToOne(optional = false)
    @JoinColumn(unique = true, nullable = false, updatable = false)
    private Benutzer user;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;
}
