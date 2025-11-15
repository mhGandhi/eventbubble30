package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.service.BenutzerService;
import com.lennadi.eventbubble30.service.VeranstaltungsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class VeranstaltungsController {

    private final VeranstaltungsService veranstaltungsService;
    private final BenutzerService benutzerService;

    // ===== DTOs =====

    public record CreateVeranstaltungRequest(
            Instant termin,
            @NotBlank String title,
            String description
    ) {}

    public record VeranstaltungsDTO(
            Instant creationDate,
            Instant termin,
            String title,
            String description
    ) {}

    private VeranstaltungsDTO toDTO(Veranstaltung vs) {
        return new VeranstaltungsDTO(
                vs.getCreationDate(),
                vs.getTermin(),
                vs.getTitle(),
                vs.getDescription()
        );
    }

    // ===== GET EVENT =====

    @GetMapping("/{id}")
    public VeranstaltungsDTO getVeranstaltungsById(@PathVariable Long id) {
        return toDTO(veranstaltungsService.getVeranstaltungById(id));
    }

    // ===== DELETE EVENT (mit Besitzprüfung – aber im Service!) =====

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVeranstaltung(@PathVariable Long id) {
        veranstaltungsService.deleteVeranstaltungById(id);
        return ResponseEntity.noContent().build();
    }

    // ===== CREATE EVENT (Besitzer = current user) =====

    @PostMapping("/create")
    public ResponseEntity<VeranstaltungsDTO> createVeranstaltung(
            @Valid @RequestBody CreateVeranstaltungRequest req
    ) {
        Veranstaltung vs = veranstaltungsService.createVeranstaltung(
                req.termin(),
                req.title(),
                req.description(),
                benutzerService.getCurrentUser() // <-- automatisch der eingeloggte User
        );

        return ResponseEntity
                .created(URI.create("/api/events/" + vs.getId()))
                .body(toDTO(vs));
    }

    // ===== LIST =====

    @GetMapping({"", "/"})
    public Page<VeranstaltungsDTO> listVeranstaltungen(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return veranstaltungsService.list(page, size).map(this::toDTO);
    }
}
