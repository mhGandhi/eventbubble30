package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.repository.VeranstaltungsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VeranstaltungServiceTest {

    private VeranstaltungsRepository repo = mock(VeranstaltungsRepository.class);
    private BenutzerService benutzerService = mock(BenutzerService.class);

    private VeranstaltungService service;

    @BeforeEach
    void setup() {
        service = new VeranstaltungService(repo, benutzerService);
    }

    // ---------------------------------------------------------
    // getVeranstaltungById
    // ---------------------------------------------------------

    @Test
    void getVeranstaltungById_notFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.getVeranstaltungById(1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getVeranstaltungById_success() {
        Veranstaltung v = new Veranstaltung();
        v.setId(2L);

        when(repo.findById(2L)).thenReturn(Optional.of(v));

        var r = service.getVeranstaltungById(2L);
        assertEquals(2L, r.getId());
    }

    // ---------------------------------------------------------
    // deleteVeranstaltungById
    // ---------------------------------------------------------

    @Test
    void deleteVeranstaltungById_forbiddenIfNotOwner() {
        Benutzer owner = new Benutzer();
        owner.setId(5L);
        Benutzer current = new Benutzer();
        current.setId(9L);

        Veranstaltung v = new Veranstaltung();
        v.setId(3L);
        v.setBesitzer(owner);

        when(repo.findById(3L)).thenReturn(Optional.of(v));
        when(benutzerService.getCurrentUser()).thenReturn(current);

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.deleteVeranstaltungById(3L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void deleteVeranstaltungById_success() {
        Benutzer owner = new Benutzer();
        owner.setId(5L);

        Veranstaltung v = new Veranstaltung();
        v.setId(8L);
        v.setBesitzer(owner);

        when(repo.findById(8L)).thenReturn(Optional.of(v));
        when(benutzerService.getCurrentUser()).thenReturn(owner);

        service.deleteVeranstaltungById(8L);

        verify(repo).delete(v);
    }

    // ---------------------------------------------------------
    // patchVeranstaltungById
    // ---------------------------------------------------------

    @Test
    void patchVeranstaltungById_notOwner() {
        Benutzer owner = new Benutzer();
        owner.setId(3L);
        Benutzer current = new Benutzer();
        current.setId(99L);

        Veranstaltung v = new Veranstaltung();
        v.setId(4L);
        v.setBesitzer(owner);

        when(repo.findById(4L)).thenReturn(Optional.of(v));
        when(benutzerService.getCurrentUser()).thenReturn(current);

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.patchVeranstaltungById(4L, null, null, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void patchVeranstaltungById_success() {
        Benutzer owner = new Benutzer();
        owner.setId(3L);

        Veranstaltung v = new Veranstaltung();
        v.setId(4L);
        v.setBesitzer(owner);
        v.setTitle("Old");
        v.setDescription("OldDesc");
        v.setTermin(Instant.parse("2024-01-01T00:00:00Z"));

        when(repo.findById(4L)).thenReturn(Optional.of(v));
        when(benutzerService.getCurrentUser()).thenReturn(owner);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Instant newTime = Instant.parse("2025-01-01T12:00:00Z");

        Veranstaltung result = service.patchVeranstaltungById(
                4L,
                newTime,
                "NewTitle",
                "NewDesc"
        );

        assertEquals("NewTitle", result.getTitle());
        assertEquals("NewDesc", result.getDescription());
        assertEquals(newTime, result.getTermin());
        assertNotNull(result.getModificationDate());
    }

    // ---------------------------------------------------------
    // createVeranstaltung
    // ---------------------------------------------------------

    @Test
    void createVeranstaltung_success() {
        Instant t = Instant.parse("2025-01-02T00:00:00Z");
        Benutzer owner = new Benutzer();
        owner.setId(123L);

        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Veranstaltung v = service.createVeranstaltung(t, "Event", "Desc", owner);

        assertEquals("Event", v.getTitle());
        assertEquals("Desc", v.getDescription());
        assertEquals(owner, v.getBesitzer());
        assertNotNull(v.getCreationDate());
        assertNotNull(v.getModificationDate());
    }

    // ---------------------------------------------------------
    // list
    // ---------------------------------------------------------

    @Test
    void list_success() {
        Veranstaltung v = new Veranstaltung();
        v.setId(1L);

        Page<Veranstaltung> page =
                new PageImpl<>(List.of(v), PageRequest.of(0, 10), 1);

        when(repo.findAll(any(PageRequest.class))).thenReturn(page);

        Page<Veranstaltung> result = service.list(0, 10);

        assertEquals(1, result.getTotalElements());
        verify(repo).findAll(argThat((PageRequest req) ->
                req.getPageNumber() == 0 &&
                        req.getPageSize() == 10 &&
                        Objects.requireNonNull(req.getSort().getOrderFor("modificationDate")).isDescending()
        ));
    }
}
