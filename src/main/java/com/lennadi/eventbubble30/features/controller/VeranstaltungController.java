package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.entities.Veranstaltung;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.features.service.VeranstaltungService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}/export.ics")
    public ResponseEntity<String> exportIcs(@PathVariable Long id) {
        String icsData = veranstaltungService.exportAsIcs(id);

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=event-"+id+".ics")
                .header("Content-Type", "text/calender; charset=utf-8")
                .body(icsData);
    }
}
