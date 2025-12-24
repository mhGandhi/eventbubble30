package com.lennadi.eventbubble30.features.service;

import com.lennadi.eventbubble30.config.ServerConfig;
import com.lennadi.eventbubble30.features.controller.VeranstaltungController;
import com.lennadi.eventbubble30.features.db.Location;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.db.entities.Veranstaltung;
import com.lennadi.eventbubble30.features.db.repository.VeranstaltungsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VeranstaltungService {
    private final VeranstaltungsRepository veranstaltungRepo;
    private final BenutzerService benutzerService;


    public Veranstaltung getVeranstaltungById(Long id) {
        return veranstaltungRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Veranstaltung mit id [" + id + "] nicht gefunden"
                ));
    }

    public String exportAsIcs(Long id) {//todo abchecken mal ka
        Veranstaltung vs = getVeranstaltungById(id);

        // ICS requires UTC timestamps in format: yyyyMMdd'T'HHmmss'Z'
        Instant start = vs.getTermin();
        String dtStart = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(java.time.ZoneOffset.UTC)
                .format(start);

        String uid = "event-" + id + "@"+ ServerConfig.DOMAIN;

        return """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//EventBubble30//EN
            BEGIN:VEVENT
            UID:%s
            DTSTAMP:%s
            DTSTART:%s
            SUMMARY:%s
            DESCRIPTION:%s
            END:VEVENT
            END:VCALENDAR
            """.formatted(
                uid,
                dtStart,
                dtStart,
                escape(vs.getTitle()),
                escape(vs.getDescription() == null ? "" : vs.getDescription())
        );
    }
    private String escape(String s) {
        return s
                .replace("\\", "\\\\")
                .replace(",", "\\,")
                .replace(";", "\\;")
                .replace("\n", "\\n");
    }


    @PreAuthorize("@authz.isEventOwner(#id) or hasRole('ADMIN')")
    public void deleteVeranstaltungById(Long id) {
        veranstaltungRepo.delete(getVeranstaltungById(id));
    }

    @PreAuthorize("@authz.isEventOwner(#id) or hasRole('ADMIN')")
    public Veranstaltung patchVeranstaltungById(Long id, Instant termin, String title, String description, Location location) {
        Veranstaltung veranstaltung = veranstaltungRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veranstaltung nicht gefunden"));

        if(termin!=null)
            veranstaltung.setTermin(termin);
        if(title!=null && !title.isBlank())
            veranstaltung.setTitle(title);
        if(description!=null)
            veranstaltung.setDescription(description);
        if(location!=null)
            veranstaltung.setLocation(location);

        return veranstaltungRepo.save(veranstaltung);
    }

    @PreAuthorize("isAuthenticated()")
    public Veranstaltung createVeranstaltung(Instant termin, String title, String description, Location loc, Benutzer besitzer){
        Veranstaltung veranstaltung = new Veranstaltung();
        veranstaltung.setTermin(termin);
        veranstaltung.setTitle(title);
        veranstaltung.setDescription(description);
        veranstaltung.setBesitzer(besitzer);
        veranstaltung.setLocation(loc);

        return veranstaltungRepo.save(veranstaltung);
    }

    public Page<Veranstaltung> search(VeranstaltungController.EventSearch s, int page, int size) {

        Specification<Veranstaltung> spec = VeranstaltungsRepository.Specs.textSearch(s.q())
                        .and(VeranstaltungsRepository.Specs.inCity(s.city()))
                        .and(VeranstaltungsRepository.Specs.inBoundingBox(
                                s.minLat(), s.minLon(),
                                s.maxLat(), s.maxLon()
                        ))
                        .and(VeranstaltungsRepository.Specs.near(
                                s.nearLat(), s.nearLon(), s.radiusKm()
                        ))
                        .and(VeranstaltungsRepository.Specs.dateBetween(s.from(), s.to()))
                        .and(VeranstaltungsRepository.Specs.ownedBy(s.ownerId()));

        Sort sort = Sort.by(
                s.orderDir() == VeranstaltungsRepository.OrderDir.asc
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                s.orderBy().name()
        );

        return veranstaltungRepo.findAll(
                spec,
                PageRequest.of(page, size, sort)
        );
    }


}
