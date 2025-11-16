package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.service.BenutzerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // ===== Endpoints =====

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

    @PatchMapping("/{id}")
    public ResponseEntity<Benutzer.DTO> patchVeranstaltung(
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


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserById(@PathVariable Long id) {
        service.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}")
    public Benutzer.DTO findUserById(@PathVariable long id) {
        return service.getById(id).toDTO();
    }

    @GetMapping("/name/{username}")
    public Benutzer.DTO findUserByUsername(@PathVariable String username) {
        return service.getByUsername(username).toDTO();
    }

    @GetMapping({"", "/"})
    public Page<Benutzer.DTO> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.list(page, size).map(Benutzer::toDTO);
    }
}
