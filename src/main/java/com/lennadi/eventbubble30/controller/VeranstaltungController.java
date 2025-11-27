package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.service.BenutzerService;
import com.lennadi.eventbubble30.service.VeranstaltungService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class VeranstaltungController {

    private final VeranstaltungService veranstaltungService;
    private final BenutzerService benutzerService;

    public record CreateVeranstaltungRequest(
            Instant termin,
            @NotEmpty String title,
            String description
    ) {}
    public record PatchVeranstaltungRequest(
            Instant termin,
            String title,
            String description
    ) {}


    @GetMapping("/{id}")
    public Veranstaltung.DTO getVeranstaltungById(@PathVariable Long id) {
        return veranstaltungService.getVeranstaltungById(id).toDTO();
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = "Veranstaltung", resourceIdParam = "id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVeranstaltung(@PathVariable Long id) {
        veranstaltungService.deleteVeranstaltungById(id);
        return ResponseEntity.noContent().build();
    }

    @Audit(action = AuditLog.Action.CREATE, resourceType = "Veranstaltung")
    @PostMapping("/create")
    public ResponseEntity<Veranstaltung.DTO> createVeranstaltung(
            @Valid @RequestBody CreateVeranstaltungRequest req
    ) {
        Veranstaltung vs = veranstaltungService.createVeranstaltung(
                req.termin(),
                req.title(),
                req.description(),
                benutzerService.getCurrentUser()
        );

        return ResponseEntity
                .created(URI.create("/api/events/" + vs.getId()))
                .body(vs.toDTO());
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = "Veranstaltung", resourceIdParam = "id")
    @PatchMapping("/{id}")
    public ResponseEntity<Veranstaltung.DTO> patchVeranstaltung(
            @PathVariable Long id,
            @Valid @RequestBody PatchVeranstaltungRequest req
    ) {
        Veranstaltung vs = veranstaltungService.patchVeranstaltungById(
                id,
                req.termin(),
                req.title(),
                req.description()
        );

        return ResponseEntity
                .ok()
                .body(vs.toDTO());
    }

    @GetMapping({"", "/"})
    public Page<Veranstaltung.DTO> listVeranstaltungen(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return veranstaltungService.list(page, size).map(Veranstaltung::toDTO);
    }
}
