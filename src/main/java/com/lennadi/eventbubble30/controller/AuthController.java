package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import com.lennadi.eventbubble30.security.CaptchaService;
import com.lennadi.eventbubble30.service.BenutzerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jdk.jshell.spi.ExecutionControl;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

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

    public record ResetPasswordRequest(
            @NotEmpty String token,
            @NotEmpty @Size(min = 8, max = 20) String newPassword
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
                .body(neu.toDTO());
    }

    @PostMapping("/login")
    public Benutzer.DTO login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            // 1. Login durchführen
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );

            // 2. SecurityContext erstellen und setzen
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            // 3. Session erzeugen und SecurityContext hineinspeichern
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );

            // 4. Benutzer laden
            Benutzer benutzer = benutzerRepository.findByUsername(req.username())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            return benutzer.toDTO();

        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    // ==== LOGOUT ====

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/validate-session")
    public ResponseEntity<Void> validateSession() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok().build();
    }

    //PW reset etc
    @PostMapping("/request-password-reset")
    public ResponseEntity<Void> requestPasswordReset(@RequestParam String email) {
        var userOpt = benutzerRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            //todo token generieren, speichern und verschicken
            //(token mit id, token, expiresAt und User neues repository etc)
        }

        // Immer 200 zurück, egal ob User existiert oder nicht
        //return ResponseEntity.ok().build();
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {

        //todo Token in Speicher finden, überprüfen ob null und ob abgelaufen, Token löschen


        //benutzerService.setPasswordById(0L/*todo userId aus Token extrahieren*/, req.newPassword);

        //return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

}