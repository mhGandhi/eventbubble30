package com.lennadi.eventbubble30.features.repository;

import com.lennadi.eventbubble30.features.entities.Benutzer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {
    public Optional<Benutzer> findByUsername(String username);
    public Optional<Benutzer> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<Benutzer> findByVerificationToken(String token);

    //todo Methoden zum Aktualisieren einzelner Felder (zB lastSeen) -> bessere Performance
}
