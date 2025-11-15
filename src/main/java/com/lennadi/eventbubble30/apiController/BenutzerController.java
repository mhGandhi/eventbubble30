package com.lennadi.eventbubble30.apiController;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.service.BenutzerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/user")
@RestController
public class BenutzerController {

    @Autowired
    private BenutzerService service;

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
    public BenutzerDTO createUser(@Valid @RequestBody CreateBenutzerRequest req) {
        return toDTO(service.createBenutzer(
                req.email(),
                req.username(),
                req.password()
        ));
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
