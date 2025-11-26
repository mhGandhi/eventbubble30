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

    public void deleteVeranstaltungById(Long id) {
        Veranstaltung v = getVeranstaltungById(id);

        Benutzer current = benutzerService.getCurrentUser();

        if (!v.getBesitzer().getId().equals(current.getId()) && !current.hasRole(Benutzer.Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Erlaubnis");
        }

        veranstaltungRepo.delete(v);
    }

    public Veranstaltung patchVeranstaltungById(Long id, Instant termin, String title, String description) {
        Veranstaltung veranstaltung = veranstaltungRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Veranstaltung nicht gefunden"));

        Benutzer current = benutzerService.getCurrentUser();
        if (!veranstaltung.getBesitzer().getId().equals(current.getId()) && !current.hasRole(Benutzer.Role.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Erlaubnis");
        }

        if(termin!=null)
            veranstaltung.setTermin(termin);
        if(title!=null && !title.isBlank())
            veranstaltung.setTitle(title);
        if(description!=null)
            veranstaltung.setDescription(description);

        return veranstaltungRepo.save(veranstaltung);
    }


    public Veranstaltung createVeranstaltung(Instant termin, String title, String description, Benutzer besitzer){
        Veranstaltung veranstaltung = new Veranstaltung();
        veranstaltung.setTermin(termin);
        veranstaltung.setTitle(title);
        veranstaltung.setDescription(description);
        veranstaltung.setBesitzer(besitzer);

        return veranstaltungRepo.save(veranstaltung);
    }

    public org.springframework.data.domain.Page<Veranstaltung> list(int page, int size) {
        return veranstaltungRepo.findAll(
                org.springframework.data.domain.PageRequest.of(page, size, Sort.by("modificationDate").descending())
        );
    }
}
