package com.lennadi.eventbubble30;

import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.security.password.PasswordResetService;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CronJobs {

    private final BenutzerService benutzerService;
    private final PasswordResetService passwordResetService;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupUnverifiedAccounts() {
        benutzerService.cleanupUnverifiedAccounts();
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupExpiredPasswordResetTokens() {
        passwordResetService.cleanupExpiredPasswordResetTokens();
    }
}
