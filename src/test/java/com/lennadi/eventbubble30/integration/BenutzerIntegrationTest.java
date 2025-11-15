package com.lennadi.eventbubble30.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)   // <<< Security deaktiviert
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class BenutzerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createUser_fullFlow() throws Exception {
        var json = """
        {
            "email": "mail@test.com",
            "username": "max",
            "password": "12345678"
        }
        """;

        mvc.perform(
                        post("/api/user/create")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andDo(document("create-user")); // <<< SNIPPET ERZEUGEN!
    }
}
