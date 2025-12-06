package com.lennadi.eventbubble30.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lennadi.eventbubble30.features.controller.BenutzerController;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BenutzerControllerTest {

    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    BenutzerService service;

    @InjectMocks
    BenutzerController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    // ---------------------------------------------------------
    // POST /api/user/create
    // ---------------------------------------------------------

    @Test
    void createUser_success() throws Exception {

        Benutzer b = new Benutzer();
        b.setId(1L);
        b.setEmail("mail@test.com");
        b.setUsername("max");

        when(service.createBenutzer(any(), any(), any()))
                .thenReturn(b);

        var json = """
        {
            "email": "mail@test.com",
            "username": "max",
            "password": "12345678"
        }
        """;

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/user/1"))
                .andExpect(jsonPath("$.id").value(1))
                //.andExpect(jsonPath("$.email").value("mail@test.com")) nicht im DTO
                .andExpect(jsonPath("$.username").value("max"));
    }

    @Test
    void createUser_validationError_missingField() throws Exception {
        var json = """
        {
            "email": "not-an-email",
            "username": "",
            "password": "123"
        }
        """;

        mockMvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------
    // PATCH /api/user/{id}
    // ---------------------------------------------------------

    @Test
    void patchUser_success() throws Exception {
        Benutzer b = new Benutzer();
        b.setId(5L);
        b.setEmail("new@test.com");
        b.setUsername("neo");

        when(service.patchBenutzerById(eq(5L), any(), any(), any()))
                .thenReturn(b);

        var json = """
        {
            "email": "new@test.com",
            "username": "neo",
            "password": "newPassword"
        }
        """;

        mockMvc.perform(patch("/api/user/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                //.andExpect(jsonPath("$.email").value("new@test.com")) nicht im DTO
                .andExpect(jsonPath("$.username").value("neo"));
    }

    @Test
    void patchUser_validationError() throws Exception {
        var json = """
        {
            "email": "invalid",
            "username": "x",
            "password": "123"
        }
        """;

        mockMvc.perform(patch("/api/user/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------
    // DELETE /api/user/{id}
    // ---------------------------------------------------------

    @Test
    void deleteUser_success() throws Exception {
        mockMvc.perform(delete("/api/user/10"))
                .andExpect(status().isNoContent());

        verify(service).deleteUserById(10L);
    }

    // ---------------------------------------------------------
    // GET /api/user/{id}
    // ---------------------------------------------------------

    @Test
    void findUserById_success() throws Exception {
        Benutzer b = new Benutzer();
        b.setId(3L);
        b.setEmail("a@b.com");
        b.setUsername("alpha");

        when(service.getById(3L)).thenReturn(b);

        mockMvc.perform(get("/api/user/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                //.andExpect(jsonPath("$.email").value("a@b.com")) nicht im DTO
                .andExpect(jsonPath("$.username").value("alpha"));
    }

    // ---------------------------------------------------------
    // GET /api/user/name/{username}
    // ---------------------------------------------------------

    @Test
    void findUserByName_success() throws Exception {
        Benutzer b = new Benutzer();
        b.setId(7L);
        b.setEmail("x@y.com");
        b.setUsername("neo");

        when(service.getByUsername("neo")).thenReturn(b);

        mockMvc.perform(get("/api/user/name/neo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("neo"));
    }

    // ---------------------------------------------------------
    // GET /api/user?page=..&size=..
    // ---------------------------------------------------------

    @Test
    void listUsers_success() throws Exception {
        Benutzer b = new Benutzer();
        b.setId(1L);
        b.setUsername("max");
        b.setEmail("a@b.de");

        Page<Benutzer> page = new PageImpl<>(
                List.of(b),
                PageRequest.of(0, 10),
                1
        );

        when(service.list(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/user?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].username").value("max"));
    }
}
