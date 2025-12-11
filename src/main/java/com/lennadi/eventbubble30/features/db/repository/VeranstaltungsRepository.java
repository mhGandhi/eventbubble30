package com.lennadi.eventbubble30.features.db.repository;

import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeranstaltungsRepository extends JpaRepository<Veranstaltung, Long> {
}
