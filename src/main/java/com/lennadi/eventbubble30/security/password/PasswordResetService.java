package com.lennadi.eventbubble30.security.password;

import com.lennadi.eventbubble30.config.ServerConfig;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.mail.EmailService;
import com.lennadi.eventbubble30.security.TokenGeneration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;

    public void requestReset(Benutzer user) {
        String token = TokenGeneration.generatePasswordResetToken();
        Instant expires = Instant.now().plus(1, ChronoUnit.HOURS);

        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(expires)
                .build();

        tokenRepo.findByUserId(user.getId()).ifPresent(tokenRepo::delete);
        tokenRepo.save(prt);

        emailService.send(//todo translate etc. (frontend reset URL) (maybe offload to frontend?)
                user.getEmail(),
                "Password Zurücksetzen",
                "\""+ServerConfig.DOMAIN+"/api/auth/reset-password\" Mit dem folgenden Http-Body aufrufen " +
                        "um das Passwort zurückzusetzen\n\n" +
                        "{\n" +
                        "\t\"token\": \"" +token+"\"\n" +
                        "\t\"newPassword\": \"[NEUES PASSWORT]\"\n" +
                        "\n}"
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

    public void cleanupExpiredPasswordResetTokens() {
        tokenRepo.deleteByExpiresAtBefore(Instant.now());
    }
}
