package com.lennadi.eventbubble30.apiController;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RequestMapping("/api/user")
@RestController
public class BenutzerController {

    @Autowired
    private BenutzerRepository benutzerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public record CreateBenutzerRequest(
            @Email @NotBlank @NotNull String email,
            @NotBlank @Size(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Benutzername darf nur Buchstaben, Zahlen und _ enthalten")
            String username,
            @Size(min = 8, max = 20) @NotNull @NotBlank String password) {};


    //todo nd ganzen benutzer returnen

    @PostMapping("/create")
    public Benutzer createUser(@Valid @RequestBody CreateBenutzerRequest benutzerRequest) {

        if (benutzerRepository.existsByUsername((benutzerRequest.username()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username existiert bereits");
        }

        if (benutzerRepository.existsByEmail(benutzerRequest.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email existiert bereits");
        }

        Benutzer neuerBenutzer = new Benutzer();
        neuerBenutzer.setEmail(benutzerRequest.email);
        neuerBenutzer.setUsername(benutzerRequest.username);
        neuerBenutzer.setPasswordHash(passwordEncoder.encode(benutzerRequest.password));

        return benutzerRepository.save(neuerBenutzer);
    }

    @GetMapping("/{id}")
    public Benutzer findUserById(@PathVariable long id) {
        return benutzerRepository.findById(id)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer mit id ["+id+"] nicht gefunden"));
    }

    @GetMapping("/name/{username}")
    public Benutzer findUserById(@PathVariable String username) {
        return benutzerRepository.findByUsername(username)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND, "Benutzer mit name ["+username+"] nicht gefunden"));
    }

    @GetMapping({"", "/"})
    public List<Benutzer> listUsers() {
        return benutzerRepository.findAll();
        //todo paginate or sth
    }
}
