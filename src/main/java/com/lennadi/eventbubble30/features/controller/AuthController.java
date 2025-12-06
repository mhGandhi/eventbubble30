package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.entities.Benutzer;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.features.repository.BenutzerRepository;
import com.lennadi.eventbubble30.security.BenutzerDetails;
import com.lennadi.eventbubble30.security.captcha.CaptchaService;
import com.lennadi.eventbubble30.security.password.PasswordResetService;
import com.lennadi.eventbubble30.security.token.JwtService;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.mail.EmailService;
import com.lennadi.eventbubble30.security.token.exceptions.TokenException;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final BenutzerRepository benutzerRepository;
    private final BenutzerService benutzerService;
    private final CaptchaService captchaService;
    private final JwtService jwtService;
    private final PasswordResetService  passwordResetService;
    private final EmailService emailService;

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
            @NotBlank @Size(min = 8, max = 20) String password,
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {

        Benutzer b = benutzerRepository.findByUsername(req.username())
                .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ungültige Anmeldedaten"));
        BenutzerDetails user;
        boolean emailVerified = b.isEmailVerified();

        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );
            user = (BenutzerDetails) auth.getPrincipal();
        } catch (DisabledException e) {
            if(emailVerified) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account nicht aktiv");
            }else{
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email nicht verifiziert");
            }
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Ungültige Anmeldedaten");
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Fehler bei der Anmeldung");
        }


        benutzerService.lastLoginDate(b.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(
                new AuthResponse(accessToken, refreshToken, b.toDTO())
        );
    }


    @Audit(action = AuditLog.Action.REFRESH, resourceType = "Benutzer")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        String refreshToken = req.refreshToken();

        try{
            BenutzerDetails details = jwtService.validateRefreshToken(refreshToken);

            String newAccess = jwtService.generateAccessToken(details);
            //todo mby System zum refresh Token rotieren?

            Benutzer benutzer = benutzerService.getById(details.getId());
            return ResponseEntity.ok(
                    new AuthResponse(newAccess, refreshToken, benutzer.toDTO())
            );
        } catch (TokenException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }

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
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestParam String email) {
        var userOpt = benutzerRepository.findByEmail(email);

        userOpt.ifPresent(passwordResetService::requestReset);

        return ResponseEntity.ok().build(); // always 200
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = "Benutzer")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            Benutzer user = passwordResetService.validateTokenAndConsume(req.token());
            benutzerService.FORCEsetPasswordById(user.getId(), req.newPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/request-email-verification")
    public ResponseEntity<Void> requestEmailVerification(@RequestParam @Email String email) {

        benutzerRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                benutzerService.sendVerificationEmail(user);
            }
        });

        return ResponseEntity.ok().build(); // always 200 for security
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = "Benutzer")
    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        benutzerService.verifyEmail(token);
        return ResponseEntity.ok("Email successfully verified.");
    }


}