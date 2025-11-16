package com.lennadi.eventbubble30.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)   // Security deaktiviert
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BenutzerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired BenutzerRepository benutzerRepo;

    @BeforeEach
    void setup() {
        // Leere DB vor jedem Test
    }

    // ------------------------------------------------------------------------------
    // CREATE USER
    // ------------------------------------------------------------------------------

    @Test
    void createUser_fullFlow() throws Exception {
        var json = """
        {
            "email": "mail@test.com",
            "username": "max",
            "password": "12345678"
        }
        """;

        mvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/user/1"))
                .andExpect(jsonPath("$.id").value(1))
                //.andExpect(jsonPath("$.email").value("mail@test.com"))
                .andExpect(jsonPath("$.username").value("max"))
                .andDo(document(
                        "user-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    // ------------------------------------------------------------------------------
    // GET USER BY ID
    // ------------------------------------------------------------------------------

    @Test
    void getUserById() throws Exception {
        Benutzer b = new Benutzer();
        b.setEmail("test@test.com");
        b.setUsername("alpha");
        b.setPasswordHash("x");
        benutzerRepo.save(b); // → bekommt ID=1

        mvc.perform(get("/api/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alpha"))
                .andDo(document(
                        "user-get",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    // ------------------------------------------------------------------------------
    // GET USER BY USERNAME
    // ------------------------------------------------------------------------------

    @Test
    void getUserByUsername() throws Exception {
        Benutzer b = new Benutzer();
        b.setEmail("u@t.com");
        b.setUsername("omega");
        b.setPasswordHash("x");
        benutzerRepo.save(b);

        mvc.perform(get("/api/user/name/omega"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("omega"))
                .andDo(document(
                        "user-by-username",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    // ------------------------------------------------------------------------------
    // PATCH USER
    // ------------------------------------------------------------------------------

    @Test
    void patchUser() throws Exception {
        Benutzer b = new Benutzer();
        b.setEmail("old@test.com");
        b.setUsername("old");
        b.setPasswordHash("x");
        benutzerRepo.save(b);

        var json = """
        {
            "email": "new@test.com",
            "username": "newname",
            "password": "newpassword"
        }
        """;

        mvc.perform(patch("/api/user/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
        // getCurrentUser() schlägt fehl → Integrationstest ignoriert Authentifizierung
    }

    // ------------------------------------------------------------------------------
    // DELETE USER
    // ------------------------------------------------------------------------------

    @Test
    void deleteUser() throws Exception {
        Benutzer b = new Benutzer();
        b.setEmail("a@b.com");
        b.setUsername("tbr");
        b.setPasswordHash("x");
        benutzerRepo.save(b);

        mvc.perform(delete("/api/user/1"))
                .andExpect(status().isNoContent())
                .andDo(document(
                        "user-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    // ------------------------------------------------------------------------------
    // LIST USERS
    // ------------------------------------------------------------------------------

    @Test
    void listUsers() throws Exception {
        benutzerRepo.save(newUser("a@test.com", "a"));
        benutzerRepo.save(newUser("b@test.com", "b"));
        benutzerRepo.save(newUser("c@test.com", "c"));

        mvc.perform(get("/api/user?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andDo(document(
                        "user-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint())
                ));
    }

    // Helper
    private Benutzer newUser(String email, String username) {
        Benutzer b = new Benutzer();
        b.setEmail(email);
        b.setUsername(username);
        b.setPasswordHash("x");
        return b;
    }
}
