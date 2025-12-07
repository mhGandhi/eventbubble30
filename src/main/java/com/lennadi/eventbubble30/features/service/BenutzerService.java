package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.config.ServerConfig;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.repository.BenutzerRepository;
import com.lennadi.eventbubble30.mail.EmailService;
import com.lennadi.eventbubble30.security.TokenGeneration;
import lombok.RequiredArgsConstructor;
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
@Transactional // All write operations benefit; read-only handled on methods below
public class BenutzerService {

    private final BenutzerRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // ========================================================================
    // Helpers
    // ========================================================================

    // Simple helper to reduce repetition
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

    // ========================================================================
    // Forced / system creation
    // ========================================================================

    public Benutzer FORCEcreateBenutzer(String email, String username, String password) {

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

    // ========================================================================
    // Internal system operations
    // ========================================================================

    public Benutzer sysGetBenutzer(long id) {
        return requireUser(id);
    }

    public void sysSetPassword(Long id, String newPassword) { // todo direct
        Benutzer b = requireUser(id);
        b.setPasswordHash(passwordEncoder.encode(newPassword));
        // save() not required; entity is managed in @Transactional
        // todo consider unifying password change logic
    }

    // ========================================================================
    // Admin creation
    // ========================================================================

    @PreAuthorize("@authz.hasRole('ADMIN')")
    public Benutzer createBenutzer(String email, String username, String password) { // todo dto?
        return FORCEcreateBenutzer(email, username, password);
    }

    // ========================================================================
    // Email verification
    // ========================================================================

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

    // ========================================================================
    // Cleanup
    // ========================================================================

    public void cleanupUnverifiedAccounts() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));

        var toDelete = repository.findAll().stream()
                .filter(u -> !u.isEmailVerified())
                .filter(u -> u.getVerificationTokenExpiresAt() != null)
                .filter(u -> u.getVerificationTokenExpiresAt().isBefore(cutoff))
                .toList();

        repository.deleteAll(toDelete);

        // todo create repository-level cleanup query (much faster)
    }

    // ========================================================================
    // Patch / update
    // ========================================================================

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public Benutzer patchBenutzerById(Long id, String email, String username, String password) {
        Benutzer b = requireUser(id);

        if (email != null && !email.isEmpty()) {
            b.setEmail(email);
        }
        if (username != null && !username.isEmpty()) {
            b.setUsername(username);
        }
        if (password != null && !password.isEmpty() &&
                !passwordEncoder.matches(password, b.getPasswordHash())) {
            b.setPasswordHash(passwordEncoder.encode(password));
        }

        // save not required
        return b;
    }

    // ========================================================================
    // Current / self user helpers
    // ========================================================================

    @PreAuthorize("@authz.isAuthenticated()")
    @Transactional(readOnly = true)
    public Benutzer getByIdOrMe(String idMe) {
        if (idMe.equalsIgnoreCase("me")) {
            return getCurrentUser();
        }
        return getById(Long.parseLong(idMe));
    }

    @PreAuthorize("@authz.isAuthenticated()")
    @Transactional(readOnly = true)
    public Benutzer getById(long id) {
        return requireUser(id);
    }

    @PreAuthorize("@authz.isAuthenticated()")
    @Transactional(readOnly = true)
    public Benutzer getByUsername(String username) {
        return requireUser(username);
    }

    @Transactional(readOnly = true)
    public Benutzer getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (ResponseStatusException ignored) {
            return null;
        }
    }

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

    // ========================================================================
    // Admin read
    // ========================================================================

    @PreAuthorize("@authz.hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Benutzer> list(int page, int size) {
        return repository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by("id").ascending())
        );
    }

    @PreAuthorize("@authz.hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public int getUserCount() {
        return (int) repository.count();
        // todo consider separate count query per role?
    }

    // ========================================================================
    // Deletion
    // ========================================================================

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public void deleteUserById(long id) {
        repository.delete(requireUser(id));
    }

    // ========================================================================
    // Validations / minor updates
    // ========================================================================

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public boolean isPasswordValidForId(Long id, String password) {
        Benutzer b = requireUser(id);
        return passwordEncoder.matches(password, b.getPasswordHash());
    }

    public void seen(Long id) { // todo insert more efficiently
        requireUser(id).setLastSeen(Instant.now());
        // save not needed
    }

    public void lastLoginDate(Long id) { // todo insert more efficiently
        requireUser(id).setLastLoginDate(Instant.now());
        // save not needed
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public void invalidateTokens(Long id) { // todo direct
        requireUser(id).setTokensInvalidatedAt(Instant.now());
        // save not needed
    }

    // todo change pw separat
    // todo find activeCount(time)
}
