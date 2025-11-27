package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import com.lennadi.eventbubble30.security.BenutzerDetails;
import com.lennadi.eventbubble30.security.captcha.CaptchaService;
import com.lennadi.eventbubble30.security.token.JwtService;
import com.lennadi.eventbubble30.service.BenutzerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final BenutzerRepository benutzerRepository;
    private final BenutzerService benutzerService;
    private final CaptchaService captchaService;
    private final JwtService jwtService;

    // ==== DTO ====

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    public record RefreshRequest(
            @NotEmpty String refreshToken
    ){}

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

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            Benutzer.DTO benutzerDTO
    ) {}

    @Audit(action = AuditLog.Action.CREATE, resourceType = "Benutzer")
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
        Benutzer neu = benutzerService.FORCEcreateBenutzer(
                req.email(),
                req.username(),
                req.password()
        );

        // 5. 201 Created zurück
        return ResponseEntity
                .created(URI.create("/api/user/" + neu.getId()))
                .body(neu.toDTO());
    }

    @Audit(action = AuditLog.Action.LOGIN, resourceType = "Benutzer")
    @PostMapping("/login")//todo require captcha (mby filter?)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );

            BenutzerDetails user = (BenutzerDetails) auth.getPrincipal();
            benutzerService.lastLoginDate(user.getId());

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            Benutzer benutzer = benutzerService.getById(user.getId());

            return ResponseEntity.ok(
                    new AuthResponse(accessToken, refreshToken, benutzer.toDTO())
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }

    @Audit(action = AuditLog.Action.REFRESH, resourceType = "Benutzer")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String refreshToken = req.refreshToken();

        Long userId = jwtService.extractUserId(refreshToken);

        Benutzer benutzer = benutzerService.getById(userId);

        BenutzerDetails details = new  BenutzerDetails(benutzer);

        jwtService.validateRefreshToken(refreshToken, details);

        String newAccess = jwtService.generateAccessToken(details);
        //todo mby System zum refresh Token rotieren?

        return ResponseEntity.ok(
                new AuthResponse(newAccess, refreshToken, benutzer.toDTO())
        );
    }

    @Audit(action = AuditLog.Action.INVALIDATE_TOKENS, resourceType = "Benutzer")
    @PostMapping("invalidate-tokens")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> invalidateOwnTokens() {
        Benutzer benutzer = benutzerService.getCurrentUser();
        benutzerService.invalidateTokens(benutzer.getId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> validateSession() {
        return ResponseEntity.ok().build();
    }

    //PW reset etc
    @Audit(action = AuditLog.Action.UPDATE, resourceType = "Benutzer")
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

    @Audit(action = AuditLog.Action.UPDATE, resourceType = "Benutzer")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {

        //todo Token in Speicher finden, überprüfen ob null und ob abgelaufen, Token löschen


        //benutzerService.setPasswordById(0L/*todo userId aus Token extrahieren*/, req.newPassword);

        //return ResponseEntity.noContent().build();
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

}