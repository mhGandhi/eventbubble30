package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

        return repository.save(user);
    }

    public Benutzer patchBenutzerById(Long id, String email, String username, String password) {
        Benutzer b = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer nicht gefunden"));

        if(!b.getId().equals(getCurrentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nicht Autorisiert");//todo oder admin
        }

        if(email!=null && !email.isEmpty()) {
            b.setEmail(email);
        }
        if(username!=null && !username.isEmpty()) {
            b.setUsername(username);
        }
        if(password!=null && !password.isEmpty()) {
            b.setPasswordHash(passwordEncoder.encode(password));
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
        repository.save(b);
    }

}
