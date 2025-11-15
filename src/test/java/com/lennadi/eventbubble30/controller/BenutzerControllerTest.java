package com.lennadi.eventbubble30.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.service.BenutzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .build(); // kein Spring-Kontext!
    }

    @Test
    void createUser_success() throws Exception {

        Benutzer b = new Benutzer();
        b.setId(1L);
        b.setEmail("mail@test.com");
        b.setUsername("max");
        b.setPasswordHash("ENC(x)");

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
                .andExpect(jsonPath("$.email").value("mail@test.com"))
                .andExpect(jsonPath("$.username").value("max"));
    }
}
