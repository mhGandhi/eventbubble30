package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.entities.Benutzer;
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
            @Email @NotBlank String email,
            @NotBlank @Size(min = 3, max = 20)
            @Pattern(regexp = "^[a-zA-Z0-9_]+$")
            String username,
            @NotBlank @Size(min = 8, max = 20)
            String password
    ) {}

    public record BenutzerDTO(
            Long id,
            String email,
            String username
    ) {}

    private BenutzerDTO toDTO(Benutzer user) {
        return new BenutzerDTO(user.getId(), user.getEmail(), user.getUsername());
    }

    // ===== Endpoints =====

    @PostMapping("/create")
    public ResponseEntity<BenutzerDTO> createUser(@Valid @RequestBody CreateBenutzerRequest req) {

        Benutzer neu = service.createBenutzer(
                req.email(),
                req.username(),
                req.password()
        );

        BenutzerDTO dto = toDTO(neu);

        return ResponseEntity
                .created(URI.create("/api/user/" + neu.getId())) // Location Header
                .body(dto);                                     // Response Body
    }


    @GetMapping("/{id}")
    public BenutzerDTO findUserById(@PathVariable long id) {
        return toDTO(service.getById(id));
    }

    @GetMapping("/name/{username}")
    public BenutzerDTO findUserByUsername(@PathVariable String username) {
        return toDTO(service.getByUsername(username));
    }

    @GetMapping({"", "/"})
    public Page<BenutzerDTO> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.list(page, size).map(this::toDTO);
    }
}
