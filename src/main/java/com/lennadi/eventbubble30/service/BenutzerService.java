package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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


    public Benutzer createBenutzer(String email, String username, String password) {

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

        return repository.save(user);
    }

    public Benutzer patchBenutzerById(Long id, String email, String username, String password) {
        Benutzer b = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden"));
        Benutzer current = getCurrentUser();

        if(!b.getId().equals(current.getId()) && !current.hasRole(Benutzer.Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Erlaubnis");
        }

        if(email!=null && !email.isEmpty()) {
            b.setEmail(email);
        }
        if(username!=null && !username.isEmpty()) {
            b.setUsername(username);
        }
        if(password!=null && !password.isEmpty()) {
            if(!passwordEncoder.matches(password, current.getPasswordHash())) {
                b.setPasswordHash(passwordEncoder.encode(password));
                b.setPasswordChangedAt(Instant.now());
            }
        }

        return repository.save(b);
    }

    public Benutzer getById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit id [" + id + "] nicht gefunden"
                ));
    }

    public Benutzer getByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Benutzer mit name [" + username + "] nicht gefunden"
                ));
    }

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
        if (auth == null
            //||
//                auth instanceof AnonymousAuthenticationToken ||
//                auth.getPrincipal() == null ||
//                "anonymousUser".equals(auth.getPrincipal())
        ) {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        String username = auth.getName();

        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public void deleteUserById(long id) {
        repository.delete(getById(id));
    }


    public boolean isPasswordValidForId(Long id, String password){
        Benutzer b = getById(id);
        return b.getPasswordHash().equals(passwordEncoder.encode(password));
    }

    public void setPasswordById(Long id, String password) {
        Benutzer b = getById(id);
        b.setPasswordHash(passwordEncoder.encode(password));
        b.setPasswordChangedAt(Instant.now());
        repository.save(b);
    }

    public void seen(Long id) {//todo insert more efficiently
        Benutzer b = getById(id);
        b.setLastSeen(Instant.now());
        repository.save(b);
    }

    public void lastLoginDate(Long id) {
        Benutzer b = getById(id);
        b.setLastLoginDate(Instant.now());
        repository.save(b);
    }

    public void invalidateTokens(Long id) {
        Benutzer b = getById(id);
        b.setTokensInvalidatedAt(Instant.now());
        repository.save(b);
    }

}
