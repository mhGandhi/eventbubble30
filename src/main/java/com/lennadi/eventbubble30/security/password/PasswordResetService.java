package com.lennadi.eventbubble30.security.password;

import com.lennadi.eventbubble30.Eventbubble30Application;
import com.lennadi.eventbubble30.config.ServerConfig;
import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.service.BenutzerService;
import com.lennadi.eventbubble30.mail.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final BenutzerService benutzerService;

    public void requestReset(Benutzer user) {
        // create token
        String token = UUID.randomUUID().toString();
        Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);

        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expires)
                .build();

        tokenRepo.save(prt);

        // send email
        String resetUrl = "https://"+ ServerConfig.DOMAIN +"/api/auth/reset-password?token=" + token;//todo different link? Frontend mäßig?

        emailService.send(//todo translate (maybe offload to frontend?)
                user.getEmail(),
                "Password Reset",
                "Click the following link to reset your password:\n\n" + resetUrl
        );
    }

    public Benutzer validateTokenAndConsume(String token) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (prt.getExpiresAt().isBefore(Instant.now())) {
            tokenRepo.delete(prt);
            throw new IllegalArgumentException("Token expired");
        }

        Benutzer user = prt.getUser();
        tokenRepo.delete(prt); // make token single-use

        return user;
    }

    @Scheduled(cron = "0 0 3 * * *")//täglich
    public void cleanupExpiredTokens() {
        tokenRepo.deleteByExpiresAtBefore(Instant.now());
    }
}
