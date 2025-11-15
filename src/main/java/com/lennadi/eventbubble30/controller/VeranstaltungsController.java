package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.repository.VeranstaltungsRepository;
import com.lennadi.eventbubble30.service.VeranstaltungsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;

@RequestMapping("/api/events")
@RestController
@RequiredArgsConstructor
public class VeranstaltungsController {
    private final VeranstaltungsService veranstaltungsService;

    /// /////////////////////////////
    public record CreateVeranstaltungRequest(
            Instant termin,
            @NotBlank String title,
            String description
    ){}
    public record VeranstaltungsDTO(
            Instant creationDate,

            Instant termin,

            @NotBlank String title,
            String description
    ){};

    public VeranstaltungsDTO toDTO(Veranstaltung vs){
        return new VeranstaltungsDTO(vs.getCreationDate(), vs.getTermin(), vs.getTitle(), vs.getDescription());
    }
    /// /////////////////////////////

    @GetMapping("/{id}")
    public VeranstaltungsDTO getVeranstaltungsById(@PathVariable Long id) {
        return toDTO(veranstaltungsService.getVeranstaltungById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVeranstaltung(@PathVariable Long id) {
        //todo authorize
        veranstaltungsService.deleteVeranstaltungById(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }


    @PostMapping("/create")
    public ResponseEntity<VeranstaltungsDTO> createVeranstaltung(@Valid @RequestBody CreateVeranstaltungRequest req){
        Veranstaltung vs = veranstaltungsService.createVeranstaltung(req.termin, req.title, req.description, null);//todo authorize
        VeranstaltungsDTO dto = toDTO(vs);
        return ResponseEntity
                .created(URI.create("/api/events/" + vs.getId())) // Location Header
                .body(dto);
    }

    @GetMapping({"", "/"})
    public Page<VeranstaltungsDTO> listVeranstaltungen(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return veranstaltungsService.list(page, size).map(this::toDTO);
    }
}
