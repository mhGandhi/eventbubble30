package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.config.ServerConfig;
import com.lennadi.eventbubble30.features.controller.BenutzerController;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.repository.BenutzerRepository;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.logging.AuditService;
import com.lennadi.eventbubble30.mail.EmailService;
import com.lennadi.eventbubble30.security.TokenGeneration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class BenutzerService {

    private final BenutzerRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Value("${cleanup.BenutzerEmailVer-d:7}")
    private int verificationDeadline;

    /// //////////////////////////////////INTERNAL

    private Benutzer requireUser(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit id [" + id + "] nicht gefunden"
                ));
    }

    private Benutzer requireUser(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit name [" + username + "] nicht gefunden"
                ));
    }

    public void resetPassword(Long id, String newPassword) {
        Benutzer b = requireUser(id);
        b.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    public Benutzer createUser(String email, String username, String password) {

        if (repository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username existiert bereits");
        }

        if (repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email existiert bereits");
        }

        Benutzer user = new Benutzer();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmailVerified(false);

        if (repository.count() == 0) { // todo stop doing that mby
            user.getRoles().add(Benutzer.Role.ADMIN);
            user.setEmailVerified(true);
        }

        return repository.save(user);
    }


    /// ////////////////////cleanup

    public void cleanupUnverifiedAccounts() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(verificationDeadline));
        int deleted = repository.cleanupUnverified(cutoff);

        auditService.logSystemAction(
                AuditLog.Action.CLEANUP_UNVERIFIED_ACCOUNTS,
                "Deleted " + deleted + " unverified accounts older than "+verificationDeadline+" days.",
                true,
                "",
                AuditLog.RType.USER,
                null
        );
    }


    ////////////////////////////////////////////////////////////Self

    @PreAuthorize("@authz.isAuthenticated()")
    @Transactional(readOnly = true)
    public Benutzer getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (ResponseStatusException ignored) {
            return null;
        }
    }

    @PreAuthorize("@authz.isAuthenticated()")
    @Transactional(readOnly = true)
    public Benutzer getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        Authentication auth = (context != null ? context.getAuthentication() : null);

        System.out.println(auth);

        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        String username = auth.getName();

        return requireUser(username);
    }

    /// ////////////////////////////////ADMIN

    @PreAuthorize("@authz.hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Benutzer> list(int page, int size) {//todo mehr pageable
        return repository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by("id").ascending())
        );
    }

    @PreAuthorize("@authz.hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public int getUserCount() {
        return (int) repository.count();
    }

    // todo find activeCount(time)

    /// //////////////////////////////////KLeine updates

    public void seen(Long id) {
        repository.updateLastSeen(id, Instant.now());
    }

    public void lastLoginDate(Long id) {
        requireUser(id).setLastLoginDate(Instant.now());
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public void invalidateTokens(Long id) {
        requireUser(id).setTokensInvalidatedAt(Instant.now());
    }

    ////////////////////////////////CRUD

    @PreAuthorize("@authz.hasRole('ADMIN')")
    public Benutzer createBenutzer(BenutzerController.CreateBenutzerRequest req) {
        return createUser(req.email(), req.username(), req.password());
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Benutzer getBenutzer(long id) {
        return requireUser(id);
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public Benutzer updateBenutzer(Long id, BenutzerController.PatchBenutzerRequest req) {
        Benutzer b = requireUser(id);
        String email = b.getEmail();
        String username = b.getUsername();

        if (email != null && !email.isEmpty()) {
            b.setEmail(email);
        }
        if (username != null && !username.isEmpty()) {
            b.setUsername(username);
        }

        return b;
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public void deleteUserById(long id) {
        repository.delete(requireUser(id));
    }

    /// //////////////////////////////PW

    @PreAuthorize("@authz.isSelf(#id)")
    public void changePassword(Long id, String oldPassword, String newPassword) {
        Benutzer b = requireUser(id);
        if(passwordEncoder.matches(oldPassword, b.getPasswordHash())) {
            resetPassword(id, newPassword);
        }else{
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect old password");
        }
    }

    @PreAuthorize("@authz.hasRole('ADMIN')")
    public void setPassword(Long id, String newPassword) {
        resetPassword(id, newPassword);
    }

    //////////////////////////////////////////////////////////MAIL

    public void sendVerificationEmail(Benutzer user) {
        if (user.isEmailVerified()) return;

        Instant expiry = Instant.now().plus(Duration.ofHours(24));

        user.setVerificationToken(TokenGeneration.generateVerificationToken());
        user.setVerificationTokenExpiresAt(expiry);

        String link = "https://" + ServerConfig.DOMAIN + "/api/auth/verify-email?token=" + user.getVerificationToken();

        emailService.send( // todo übersetzen etc
                user.getEmail(),
                "Email Verifizieren",
                "Link aufrufen zum verifizieren (gültig bis " + expiry + "):\n\n" + link
        );

        // todo send async after commit?
    }

    public Benutzer verifyEmail(String token) {
        Benutzer b = repository.findByVerificationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (b.getVerificationTokenExpiresAt() != null &&
                b.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        b.setEmailVerified(true);
        b.setVerificationToken(null);
        b.setVerificationTokenExpiresAt(null);

        // save not required (dirty checking)
        return b;
    }

}
