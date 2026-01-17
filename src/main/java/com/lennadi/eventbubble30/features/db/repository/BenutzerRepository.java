package com.lennadi.eventbubble30.features.db.repository;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {
    public Optional<Benutzer> findByUsername(String username);
    public Optional<Benutzer> findByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    Optional<Benutzer> findByVerificationToken(String token);

    @Modifying
    @Query("UPDATE Benutzer b SET b.lastSeen = :ts WHERE b.id = :id")
    void updateLastSeen(@Param("id") Long id, @Param("ts") Instant ts);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
DELETE FROM Benutzer b
WHERE b.emailVerified = false
  AND b.creationDate < :cutoff
""")
    int cleanupUnverified(@Param("cutoff") Instant cutoff);


}
