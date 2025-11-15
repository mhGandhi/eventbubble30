package com.lennadi.eventbubble30.repository;

import com.lennadi.eventbubble30.entities.Veranstaltung;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VeranstaltungsRepository extends JpaRepository<Veranstaltung, Long> {
}
