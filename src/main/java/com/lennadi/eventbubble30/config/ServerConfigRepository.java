package com.lennadi.eventbubble30.config;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerConfigRepository extends JpaRepository<ServerConfigSingletonEntity, Long> {
}
