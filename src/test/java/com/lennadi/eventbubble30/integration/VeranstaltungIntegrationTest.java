package com.lennadi.eventbubble30.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lennadi.eventbubble30.TestConfig;
import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import com.lennadi.eventbubble30.repository.VeranstaltungsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VeranstaltungIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Autowired BenutzerRepository benutzerRepo;
    @Autowired VeranstaltungsRepository veranstaltungsRepo;

    @BeforeEach
    void setup() {
        // Globale „current user“
        Benutzer b = new Benutzer();
        b.setEmail("owner@test.com");
        b.setUsername("owner");
        b.setPasswordHash("x");
        benutzerRepo.save(b); // → bekommt ID = 1
    }

    // ---------------------------------------------------------
    // CREATE EVENT
    // ---------------------------------------------------------

    @Test
    void createVeranstaltung_fullFlow() throws Exception {
        String json = """
        {
            "termin": "2025-01-01T12:00:00Z",
            "title": "Silvesterparty",
            "description": "Große Feier"
        }
        """;

        mvc.perform(post("/api/events/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/events/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Silvesterparty"))
                .andExpect(jsonPath("$.description").value("Große Feier"))
                .andDo(document("event-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    // ---------------------------------------------------------
    // GET EVENT BY ID
    // ---------------------------------------------------------

    @Test
    void getVeranstaltungById() throws Exception {
        // zuerst Event anlegen
        var event = veranstaltungsRepo.save(
                createEvent("Test", "Desc", Instant.parse("2025-02-01T12:00:00Z"))
        );

        mvc.perform(get("/api/events/" + event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId()))
                .andExpect(jsonPath("$.title").value("Test"))
                .andDo(document("event-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    // ---------------------------------------------------------
    // PATCH EVENT
    // ---------------------------------------------------------

    @Test
    void patchVeranstaltung() throws Exception {
        var event = veranstaltungsRepo.save(
                createEvent("Alt", "AltBeschreibung", Instant.parse("2025-03-01T12:00:00Z"))
        );

        String json = """
        {
            "termin": "2025-03-05T18:00:00Z",
            "title": "NeuTitel",
            "description": "Neu Desc"
        }
        """;

        mvc.perform(patch("/api/events/" + event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("NeuTitel"))
                .andExpect(jsonPath("$.description").value("Neu Desc"))
                .andDo(document("event-patch",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    // ---------------------------------------------------------
    // DELETE EVENT
    // ---------------------------------------------------------

    @Test
    void deleteVeranstaltung() throws Exception {
        var event = veranstaltungsRepo.save(
                createEvent("Zum Löschen", "Desc", Instant.parse("2025-04-01T12:00:00Z"))
        );

        mvc.perform(delete("/api/events/" + event.getId()))
                .andExpect(status().isNoContent())
                .andDo(document("event-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    // ---------------------------------------------------------
    // LIST EVENTS
    // ---------------------------------------------------------

    @Test
    void listVeranstaltungen() throws Exception {
        veranstaltungsRepo.save(createEvent("A", "D1", Instant.now()));
        veranstaltungsRepo.save(createEvent("B", "D2", Instant.now()));
        veranstaltungsRepo.save(createEvent("C", "D3", Instant.now()));

        mvc.perform(get("/api/events?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andDo(document("event-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())));
    }

    // ---------------------------------------------------------
    // Helper
    // ---------------------------------------------------------

    private com.lennadi.eventbubble30.entities.Veranstaltung createEvent(
            String title, String desc, Instant termin
    ) {
        var owner = benutzerRepo.findById(1L).orElseThrow();

        var v = new com.lennadi.eventbubble30.entities.Veranstaltung();
        v.setTitle(title);
        v.setDescription(desc);
        v.setTermin(termin);
        v.setBesitzer(owner);
        return v;
    }
}
