package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BenutzerService {

    private final BenutzerRepository repository;
    private final PasswordEncoder passwordEncoder;

    @PreAuthorize("hasRole('ADMIN')")
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
        user.setPasswordChangedAt(Instant.now());

        if(repository.count() == 0) {
            user.getRoles().add(Benutzer.Role.ADMIN);
        }

        return repository.save(user);
    }

    @PreAuthorize("@authz.isSelf(#id) or hasRole('ADMIN')")
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
            b.setPasswordChangedAt(Instant.now());
        }

        return repository.save(b);
    }

    @PreAuthorize("isAuthenticated()")
    public Benutzer getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit id [" + id + "] nicht gefunden"
                ));
    }

    @PreAuthorize("isAuthenticated()")
    public Benutzer getByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit name [" + username + "] nicht gefunden"
                ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.data.domain.Page<Benutzer> list(int page, int size) {
        return repository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by("id").ascending())
        );
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

    @PreAuthorize("@authz.isSelf(#id) or hasRole('ADMIN')")
    public void deleteUserById(long id) {
        repository.delete(getById(id));
    }

    @PreAuthorize("@authz.isSelf(#id) or hasRole('ADMIN')")
    public boolean isPasswordValidForId(Long id, String password){
        Benutzer b = getById(id);
        return passwordEncoder.matches(password, b.getPasswordHash());
    }

    @PreAuthorize("@authz.isSelf(#id) or hasRole('ADMIN')")
    public void setPasswordById(Long id, String password) {
        FORCEsetPasswordById(id, password);
    }

    public void FORCEsetPasswordById(Long id, String newPassword) {
        Benutzer b = getById(id);
        b.setPasswordHash(passwordEncoder.encode(newPassword));
        b.setPasswordChangedAt(Instant.now());
        repository.save(b);
    }

    @PreAuthorize("@authz.isSelf(#id)")
    public void seen(Long id) {//todo insert more efficiently
        Benutzer b = getById(id);
        b.setLastSeen(Instant.now());
        repository.save(b);
    }

    @PreAuthorize("@authz.isSelf(#id)")
    public void lastLoginDate(Long id) {
        Benutzer b = getById(id);
        b.setLastLoginDate(Instant.now());
        repository.save(b);
    }

    @PreAuthorize("@authz.isSelf(#id) or hasRole('ADMIN')")
    public void invalidateTokens(Long id) {
        Benutzer b = getById(id);
        b.setTokensInvalidatedAt(Instant.now());
        repository.save(b);
    }

}
