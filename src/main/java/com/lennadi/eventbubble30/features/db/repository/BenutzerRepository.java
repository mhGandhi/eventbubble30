package com.lennadi.eventbubble30.features.db.repository;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BenutzerRepository extends JpaRepository<Benutzer, Long> {
    public Optional<Benutzer> findByUsernameIgnoreCase(String username);

    public Optional<Benutzer> findByExternalIdIgnoreCase(String externalId);
    public Optional<Benutzer> findByEmailIgnoreCase(@NotBlank String username);

    boolean existsByEmail(String email);
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByIdAndBookmarkedVeranstaltungen_ExternalId(Long userId, String eventExternalId);

    @Query("""
        select v
        from Benutzer b join b.bookmarkedVeranstaltungen v
        where b.externalId = :extId
        """)
    Set<Veranstaltung> findBookmarkedEvents(@Param("extId") String extId);

    /*
    @Query("""
        select count(v)
        from Benutzer b join b.bookmarkedVeranstaltungen v
        where v.externalId = :eventExternalId
        """)
    long bookmarkCount(@Param("eventExternalId") String eventExternalId);
    public record EventCount(String eventExternalId, long count) {}

    @Query("""
        select new com.yourpkg.EventCount(v.externalId, count(b))
        from Benutzer b join b.bookmarkedVeranstaltungen v
        where v.externalId in :eventExternalIds
        group by v.externalId
        """)
    List<EventCount> bookmarkCounts(@Param("eventExternalIds") Collection<String> eventExternalIds);
    */

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
