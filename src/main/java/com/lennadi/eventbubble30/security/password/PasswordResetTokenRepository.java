package com.lennadi.eventbubble30.security.password;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByExpiresAtBefore(Instant now);

    Optional<PasswordResetToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
