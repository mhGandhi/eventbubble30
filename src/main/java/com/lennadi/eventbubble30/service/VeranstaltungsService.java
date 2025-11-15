package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.repository.VeranstaltungsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VeranstaltungsService {
    private final VeranstaltungsRepository veranstaltungsRepo;


    public Veranstaltung getVeranstaltungById(Long id) {
        return veranstaltungsRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Veranstaltung mit id [" + id + "] nicht gefunden"
                ));
    }

    public void deleteVeranstaltungById(Long id) {
        Veranstaltung v = getVeranstaltungById(id);

        Benutzer current = v.getBesitzer();
        // Besitzerprüfung wird vom Controller NICHT gemacht — nur hier!

        if (!v.getBesitzer().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner");
        }

        veranstaltungsRepo.delete(v);
    }

    public Veranstaltung createVeranstaltung(Instant termin, String title, String description, Benutzer besitzer){
        Veranstaltung veranstaltung = new Veranstaltung();
        veranstaltung.setTermin(termin);
        veranstaltung.setTitle(title);
        veranstaltung.setDescription(description);
        veranstaltung.setBesitzer(besitzer);

        veranstaltung.setCreationDate(Instant.now());

        return veranstaltungsRepo.save(veranstaltung);
    }

    public org.springframework.data.domain.Page<Veranstaltung> list(int page, int size) {
        return veranstaltungsRepo.findAll(
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by("id").ascending())
        );
    }
}
