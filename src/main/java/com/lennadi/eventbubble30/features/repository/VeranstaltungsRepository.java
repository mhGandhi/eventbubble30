package com.lennadi.eventbubble30.features.repository;

import com.lennadi.eventbubble30.features.entities.Veranstaltung;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeranstaltungsRepository extends JpaRepository<Veranstaltung, Long> {
}
