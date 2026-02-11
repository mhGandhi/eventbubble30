package com.lennadi.eventbubble30.config;

import com.lennadi.eventbubble30.features.db.EntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter @Setter
public class ServerConfigSingletonEntity {
    public static final EntityType TYPE = EntityType.SERVER_CONFIG;

    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private Long id = 1L;

    @Column(nullable = false)
    private Instant globalTokensInvalidatedAt = Instant.EPOCH;
}
