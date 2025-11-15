package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.dto.BenutzerDTO;
import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import com.lennadi.eventbubble30.security.CaptchaService;
import com.lennadi.eventbubble30.service.BenutzerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final BenutzerRepository benutzerRepository;
    private final BenutzerService benutzerService;
    private final CaptchaService captchaService;

    // ==== DTO ====

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record SignupRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank @Email String email,
            @NotBlank String captchaToken
    ) {}


    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {

        // 1. CAPTCHA prüfen
        if (!captchaService.verify(req.captchaToken())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid CAPTCHA");
        }

        // 2. prüfen ob username schon existiert
        if (benutzerRepository.existsByUsername(req.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Username already taken");
        }

        // 3. prüfen ob email existiert
        if (benutzerRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Email already taken");
        }

        // 4. Benutzer erstellen
        Benutzer neu = benutzerService.createBenutzer(
                req.email(),
                req.username(),
                req.password()
        );

        // 5. 201 Created zurück
        return ResponseEntity
                .created(URI.create("/api/user/" + neu.getId()))
                .body(BenutzerDTO.fromBenutzer(neu));
    }

    @PostMapping("/login")
    public BenutzerDTO login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            // Passwort prüfen
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );


            SecurityContextHolder.getContext().setAuthentication(auth);//todo in redis saven
            request.getSession(true); // Session erzeugen

            // Benutzer aus DB holen
            Benutzer benutzer = benutzerRepository.findByUsername(req.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            return BenutzerDTO.fromBenutzer(benutzer);

        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }


    // ==== LOGOUT ====

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.noContent().build(); // 204 No Content
    }


    // ==== CURRENT USER ====

    @GetMapping("/me")
    public BenutzerDTO me() {
        Benutzer benutzer = benutzerService.getCurrentUser();
        return BenutzerDTO.fromBenutzer(benutzer);
    }
}