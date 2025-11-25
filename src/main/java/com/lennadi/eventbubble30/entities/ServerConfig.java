package com.lennadi.eventbubble30.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
public class ServerConfig {
    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private Long id = 1L;

    @Getter
    @Setter
    @Column(nullable = false)
    private Instant globalTokensInvalidatedAt = Instant.EPOCH;
}
