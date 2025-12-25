package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.db.Location;
import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import com.lennadi.eventbubble30.features.db.repository.VeranstaltungsRepository;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.features.service.VeranstaltungService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            String description,
            Location location
    ) {}
    public record PatchVeranstaltungRequest(
            Instant termin,
            @NotBlank String title,
            String description,
            Location location
    ) {}

    public record EventSearch(
            String q,
            String city,

            // bounding box
            Double minLat,
            Double minLon,
            Double maxLat,
            Double maxLon,

            // radius search (Quadrat glaube)
            Double nearLat,
            Double nearLon,
            Double radiusKm,

            Instant from,
            Instant to,

            Long ownerId,

            VeranstaltungsRepository.OrderBy orderBy,
            VeranstaltungsRepository.OrderDir orderDir
    ) {}

    @GetMapping("/{id}")
    public Veranstaltung.DTO getVeranstaltungById(@PathVariable Long id) {
        return veranstaltungService.getVeranstaltungById(id).toDTO();
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = AuditLog.RType.EVENT, resourceIdParam = "id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVeranstaltung(@PathVariable Long id) {
        veranstaltungService.deleteVeranstaltungById(id);
        return ResponseEntity.noContent().build();
    }

    @Audit(action = AuditLog.Action.CREATE, resourceType = AuditLog.RType.EVENT)
    @PostMapping("/create")
    public ResponseEntity<Veranstaltung.DTO> createVeranstaltung(
            @Valid @RequestBody CreateVeranstaltungRequest req
    ) {
        Veranstaltung vs = veranstaltungService.createVeranstaltung(
                req.termin(),
                req.title(),
                req.description(),
                req.location(),
                benutzerService.getCurrentUser()
        );

        return ResponseEntity
                .created(URI.create("/api/events/" + vs.getId()))
                .body(vs.toDTO());
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = AuditLog.RType.EVENT, resourceIdParam = "id")
    @PatchMapping("/{id}")
    public ResponseEntity<Veranstaltung.DTO> patchVeranstaltung(
            @PathVariable Long id,
            @Valid @RequestBody PatchVeranstaltungRequest req
    ) {
        Veranstaltung vs = veranstaltungService.patchVeranstaltungById(
                id,
                req.termin(),
                req.title(),
                req.description(),
                req.location()
        );

        return ResponseEntity
                .ok()
                .body(vs.toDTO());
    }

    @GetMapping({"", "/"})
    public Page<Veranstaltung.DTO> listVeranstaltungen(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String bbox,
            @RequestParam(required = false) String near,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long owner,//todo für User ohne Profil Datenschutz ermöglichen
            @RequestParam(defaultValue = "termin") VeranstaltungsRepository.OrderBy orderBy,
            @RequestParam(defaultValue = "asc") VeranstaltungsRepository.OrderDir orderDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        EventSearch search = parseSearch(
                q, city, bbox, near, from, to, owner, orderBy, orderDir
        );

        return veranstaltungService
                .search(search, page, size)
                .map(Veranstaltung::toDTO);
    }

    private static final double MAX_RADIUS_KM = 100.0;
    private static final int MAX_PAGE_SIZE = 100;

    private EventSearch parseSearch(
            String q,
            String city,
            String bbox,
            String near,
            Instant from,
            Instant to,
            Long owner,
            VeranstaltungsRepository.OrderBy orderBy,
            VeranstaltungsRepository.OrderDir orderDir
    ) {
        Double minLat = null, minLon = null, maxLat = null, maxLon = null;
        Double nearLat = null, nearLon = null, radiusKm = null;

        // --- bbox parsing ---
        if (bbox != null) {
            String[] p = bbox.split(",");
            if (p.length != 4) {
                badRequest("bbox must be 'minLon,minLat,maxLon,maxLat'");
            }

            try {
                minLon = Double.parseDouble(p[0]);
                minLat = Double.parseDouble(p[1]);
                maxLon = Double.parseDouble(p[2]);
                maxLat = Double.parseDouble(p[3]);
            } catch (NumberFormatException e) {
                badRequest("bbox values must be valid numbers");
            }

            validateLatLon(minLat, minLon);
            validateLatLon(maxLat, maxLon);

            if (minLat > maxLat || minLon > maxLon) {
                badRequest("bbox min values must be <= max values");
            }
        }

        // --- near parsing ---
        if (near != null) {
            String[] p = near.split(",");
            if (p.length != 3) {
                badRequest("near must be 'lat,lon,radiusKm'");
            }

            try {
                nearLat = Double.parseDouble(p[0]);
                nearLon = Double.parseDouble(p[1]);
                radiusKm = Double.parseDouble(p[2]);
            } catch (NumberFormatException e) {
                badRequest("near values must be valid numbers");
            }

            validateLatLon(nearLat, nearLon);

            if (radiusKm <= 0) {
                badRequest("radiusKm must be > 0");
            }
            if (radiusKm > MAX_RADIUS_KM) {
                badRequest("radiusKm must be <= " + MAX_RADIUS_KM);
            }
        }

        // --- date validation ---
        if (from != null && to != null && from.isAfter(to)) {
            badRequest("'from' must be <= 'to'");
        }

        return new EventSearch(
                q,
                city,
                minLat, minLon, maxLat, maxLon,
                nearLat, nearLon, radiusKm,
                from, to,
                owner,
                orderBy,
                orderDir
        );
    }

    private static void validateLatLon(double lat, double lon) {
        if (lat < -90 || lat > 90) {
            badRequest("latitude must be between -90 and 90");
        }
        if (lon < -180 || lon > 180) {
            badRequest("longitude must be between -180 and 180");
        }
    }

    private static void badRequest(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }


    @GetMapping("/{id}/export.ics")
    public ResponseEntity<String> exportIcs(@PathVariable Long id) {
        String icsData = veranstaltungService.exportAsIcs(id);

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=event-"+id+".ics")
                .header("Content-Type", "text/calendar; charset=utf-8")
                .body(icsData);
    }
}
