package com.lennadi.eventbubble30;

import com.lennadi.eventbubble30.features.repository.BenutzerRepository;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    BenutzerService mockedBenutzerService(BenutzerRepository repo) {

        BenutzerService mock = Mockito.mock(BenutzerService.class);

        // Default: "current user" ist User mit ID 1 (wird im Test angelegt)
        Mockito.when(mock.getCurrentUser())
                .thenAnswer(inv -> repo.findById(1L).orElse(null));

        return mock;
    }
}
