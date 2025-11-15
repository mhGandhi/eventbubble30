package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.repository.BenutzerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BenutzerServiceTest {

    private final BenutzerRepository repo = mock(BenutzerRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private final BenutzerService service = new BenutzerService(repo, encoder);

    @Test
    void createUser_success() {
        when(repo.existsByEmail("mail@test.com")).thenReturn(false);
        when(repo.existsByUsername("testuser")).thenReturn(false);
        when(encoder.encode("secret")).thenReturn("ENC(secret)");

        Benutzer saved = new Benutzer();
        saved.setId(1L);
        saved.setEmail("mail@test.com");
        saved.setUsername("testuser");
        saved.setPasswordHash("ENC(secret)");

        when(repo.save(any())).thenReturn(saved);

        var result = service.createBenutzer("mail@test.com", "testuser", "secret");

        assertEquals(1L, result.getId());
        assertEquals("mail@test.com", result.getEmail());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void createUser_emailTaken() {
        when(repo.existsByEmail("mail@test.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createBenutzer("mail@test.com", "testuser", "secret")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void getById_notFound() {
        when(repo.findById(5L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getById(5)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
