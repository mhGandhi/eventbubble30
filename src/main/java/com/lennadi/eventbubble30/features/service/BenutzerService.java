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
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BenutzerService {

    private final BenutzerRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @PreAuthorize("@authz.hasRole('ADMIN')")
    public Benutzer createBenutzer(String email, String username, String password) {
        return FORCEcreateBenutzer(email, username, password);
    }

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

        if(repository.count() == 0) {//todo stop doing that mby
            user.getRoles().add(Benutzer.Role.ADMIN);
            user.setEmailVerified(true);
        }

        return repository.save(user);
    }


    public void sendVerificationEmail(Benutzer user) {
        if (user.isEmailVerified()) return;

        Instant expiry = Instant.now().plus(Duration.ofHours(24));

        user.setVerificationToken(TokenGeneration.generateVerificationToken());
        user.setVerificationTokenExpiresAt(expiry);
        repository.save(user);

        String link = "https://"+ ServerConfig.DOMAIN+"/api/auth/verify-email?token=" + user.getVerificationToken();

        emailService.send(//todo übersetzen etc
                user.getEmail(),
                "Email Verifizieren",
                "Link aufrufen zum verifizieren (gültig bis "+expiry+"):\n\n" + link
        );
    }

    public void verifyEmail(String token) {
        Benutzer b = repository.findByVerificationToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (b.getVerificationTokenExpiresAt() != null &&
                b.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token expired");
        }

        b.setEmailVerified(true);
        b.setVerificationToken(null);
        b.setVerificationTokenExpiresAt(null);

        repository.save(b);
    }

    public void cleanupUnverifiedAccounts() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));

        var toDelete = repository.findAll().stream()
                .filter(u -> !u.isEmailVerified())
                .filter(u -> u.getVerificationTokenExpiresAt() != null)
                .filter(u -> u.getVerificationTokenExpiresAt().isBefore(cutoff))
                .toList();

        repository.deleteAll(toDelete);
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public Benutzer patchBenutzerById(Long id, String email, String username, String password) {
        Benutzer b = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden"));

        if(email!=null && !email.isEmpty()) {
            b.setEmail(email);
        }
        if(username!=null && !username.isEmpty()) {
            b.setUsername(username);
        }
        if(password!=null && !password.isEmpty() && !passwordEncoder.matches(password, b.getPasswordHash())) {
            b.setPasswordHash(passwordEncoder.encode(password));
        }

        return repository.save(b);
    }

    @PreAuthorize("@authz.isAuthenticated()")
    public Benutzer getByIdOrMe(String idMe){
        if(idMe.equalsIgnoreCase("me")){
            return getCurrentUser();
        }else{
            return getById(Long.parseLong(idMe));
        }
    }

    @PreAuthorize("@authz.isAuthenticated()")
    public Benutzer getById(long id) {
        return FORCEgetById(id);
    }

    public Benutzer FORCEgetById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit id [" + id + "] nicht gefunden"
                ));
    }

    @PreAuthorize("@authz.isAuthenticated()")
    public Benutzer getByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit name [" + username + "] nicht gefunden"
                ));
    }

    @PreAuthorize("@authz.hasRole('ADMIN')")
    public org.springframework.data.domain.Page<Benutzer> list(int page, int size) {
        return repository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by("id").ascending())
        );
    }

    public Benutzer getCurrentUserOrNull(){
        try{
            return getCurrentUser();
        }catch(ResponseStatusException ignored){
            return null;
        }
    }

    public Benutzer getCurrentUser() {
        var context = SecurityContextHolder.getContext();
        Authentication auth = null;
        if(context != null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }

        System.out.println(auth);
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        String username = auth.getName();

        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public void deleteUserById(long id) {
        repository.delete(getById(id));
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public boolean isPasswordValidForId(Long id, String password){
        Benutzer b = getById(id);
        return passwordEncoder.matches(password, b.getPasswordHash());
    }

    public void FORCEsetPasswordById(Long id, String newPassword) {
        Benutzer b = FORCEgetById(id);
        b.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(b);
    }

    public void seen(Long id) {//todo insert more efficiently
        Benutzer b = FORCEgetById(id);
        b.setLastSeen(Instant.now());
        repository.save(b);
    }

    public void lastLoginDate(Long id) {
        Benutzer b = FORCEgetById(id);
        b.setLastLoginDate(Instant.now());
        repository.save(b);
    }

    @PreAuthorize("@authz.isSelf(#id) or @authz.hasRole('ADMIN')")
    public void invalidateTokens(Long id) {
        Benutzer b = getById(id);
        b.setTokensInvalidatedAt(Instant.now());
        repository.save(b);
    }

    @PreAuthorize("@authz.hasRole('ADMIN')")
    public int getUserCount() {
        return repository.findAll().size();
    }
}
