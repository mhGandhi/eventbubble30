package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.entities.Benutzer;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RequestMapping("/api/user")
@RestController
@RequiredArgsConstructor
public class BenutzerController {

    private final BenutzerService service;

    // ===== DTOs =====

    public record CreateBenutzerRequest(
            @Email @NotEmpty String email,
            @NotEmpty @Size(min = 3, max = 20)
            @Pattern(regexp = "^[a-zA-Z0-9_]+$")
            String username,
            @NotEmpty @Size(min = 8, max = 20)
            String password
    ) {}

    public record PatchBenutzerRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 3, max = 20)
            @Pattern(regexp = "^[a-zA-Z0-9_]+$")
            String username,
            @NotBlank @Size(min = 8, max = 20)
            String password
    ) {}

    public record UpdateOwnPasswordRequest(
            @NotEmpty
            String oldPassword,
            @NotEmpty @Size(min = 8, max = 20)
            String newPassword
    ) {}

    // ===== Endpoints =====

    @Audit(action = AuditLog.Action.CREATE, resourceType = "Benutzer")
    @PostMapping("/create")
    public ResponseEntity<Benutzer.DTO> createUser(@Valid @RequestBody CreateBenutzerRequest req) {

        Benutzer neu = service.createBenutzer(
                req.email(),
                req.username(),
                req.password()
        );

        return ResponseEntity
                .created(URI.create("/api/user/" + neu.getId())) // Location Header
                .body(neu.toDTO());                                     // Response Body
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = "Benutzer", resourceIdParam = "id")
    @PatchMapping("/{id}")
    public ResponseEntity<Benutzer.DTO> patchUser(
            @PathVariable Long id,
            @Valid @RequestBody PatchBenutzerRequest req
    ) {
        Benutzer b = service.patchBenutzerById(
                id,
                req.email,
                req.username,
                req.password
        );

        return ResponseEntity
                .ok()
                .body(b.toDTO());
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = "Benutzer", resourceIdParam = "id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        service.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    //@Audit(action = AuditLog.Action.READ, resourceType = "Benutzer", resourceIdParam = "id")
    @GetMapping("/{id}")
    public Benutzer.DTO findUserById(@PathVariable long id) {
        return service.getById(id).toDTO();
    }


    //@Audit(action = AuditLog.Action.READ, resourceType = "Benutzer")
    @GetMapping({"", "/"})
    public Page<Benutzer.DTO> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.list(page, size).map(Benutzer::toDTO);
    }

    //@Audit(action = AuditLog.Action.READ, resourceType = "Benutzer", resourceIdParam = "currentUser")
    @GetMapping("/me")
    public Benutzer.DTO me() {
        Benutzer benutzer = service.getCurrentUser();
        return benutzer.toDTO();
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = "Benutzer", resourceIdParam = "currentUser")
    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody UpdateOwnPasswordRequest req
    ) {
        Benutzer current = service.getCurrentUser();

        if(!service.isPasswordValidForId(current.getId(), req.oldPassword)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect old password");
        }

        service.setPasswordById(current.getId(), req.newPassword);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public int getUserCount() {
        return service.getUserCount();
    }

}
