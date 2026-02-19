package com.lennadi.eventbubble30.features.db.repository;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {
    public Optional<Benutzer> findByUsernameIgnoreCase(String username);

    public Optional<Benutzer> findByExternalIdIgnoreCase(String externalId);
    public Optional<Benutzer> findByEmailIgnoreCase(@NotBlank String username);

    boolean existsByEmail(String email);
    boolean existsByUsernameIgnoreCase(String username);

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
