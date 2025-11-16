package com.lennadi.eventbubble30.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.entities.Veranstaltung;
import com.lennadi.eventbubble30.service.BenutzerService;
import com.lennadi.eventbubble30.service.VeranstaltungService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VeranstaltungControllerTest {

    MockMvc mockMvc;
    ObjectMapper mapper = new ObjectMapper();

    @Mock
    VeranstaltungService veranstaltungService;

    @Mock
    BenutzerService benutzerService;

    @InjectMocks
    VeranstaltungController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ---------------------------------------------------------
    // GET /api/events/{id}
    // ---------------------------------------------------------

    @Test
    void getVeranstaltungById_success() throws Exception {
        Veranstaltung v = new Veranstaltung();
        v.setId(10L);
        v.setTitle("Party");
        v.setTermin(Instant.parse("2025-01-01T12:00:00Z"));

        when(veranstaltungService.getVeranstaltungById(10L)).thenReturn(v);

        mockMvc.perform(get("/api/events/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Party"));
    }

    // ---------------------------------------------------------
    // DELETE /api/events/{id}
    // ---------------------------------------------------------

    @Test
    void deleteVeranstaltung_success() throws Exception {
        mockMvc.perform(delete("/api/events/7"))
                .andExpect(status().isNoContent());

        verify(veranstaltungService).deleteVeranstaltungById(7L);
    }

    // ---------------------------------------------------------
    // POST /api/events/create
    // ---------------------------------------------------------

    @Test
    void createVeranstaltung_success() throws Exception {
        Instant ts = Instant.parse("2025-01-01T10:00:00Z");

        Benutzer owner = new Benutzer();
        owner.setId(3L);
        owner.setUsername("max");

        Veranstaltung created = new Veranstaltung();
        created.setId(55L);
        created.setTitle("Geburtstag");
        created.setDescription("Feier");
        created.setTermin(ts);
        //created.setOwner(owner);

        when(benutzerService.getCurrentUser()).thenReturn(owner);
        when(veranstaltungService.createVeranstaltung(any(), any(), any(), eq(owner)))
                .thenReturn(created);

        var json = """
        {
            "termin": "2025-01-01T10:00:00Z",
            "title": "Geburtstag",
            "description": "Feier"
        }
        """;

        mockMvc.perform(post("/api/events/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/events/55"))
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.title").value("Geburtstag"));
    }

    @Test
    void createVeranstaltung_validationError() throws Exception {
        var json = """
        {
            "termin": null,
            "title": "",
            "description": "X"
        }
        """;

        mockMvc.perform(post("/api/events/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------
    // PATCH /api/events/{id}
    // ---------------------------------------------------------

    @Test
    void patchVeranstaltung_success() throws Exception {
        Instant time = Instant.parse("2025-02-01T15:00:00Z");

        Veranstaltung v = new Veranstaltung();
        v.setId(8L);
        v.setTitle("Neu");
        v.setTermin(time);

        when(veranstaltungService.patchVeranstaltungById(eq(8L), any(), any(), any()))
                .thenReturn(v);

        var json = """
        {
            "termin": "2025-02-01T15:00:00Z",
            "title": "Neu",
            "description": "Aktualisiert"
        }
        """;

        mockMvc.perform(patch("/api/events/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.title").value("Neu"));
    }

    // ---------------------------------------------------------
    // GET /api/events
    // ---------------------------------------------------------

    @Test
    void listVeranstaltungen_success() throws Exception {
        Veranstaltung v = new Veranstaltung();
        v.setId(1L);
        v.setTitle("Event X");

        Page<Veranstaltung> page =
                new PageImpl<>(List.of(v), PageRequest.of(0, 10), 1);

        when(veranstaltungService.list(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/events?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Event X"));
    }
}
