package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.features.entities.Benutzer;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import com.lennadi.eventbubble30.features.repository.BenutzerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BenutzerServiceTest {

    private final BenutzerRepository repo = mock(BenutzerRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private BenutzerService service;

    private MockedStatic<SecurityContextHolder> securityMock;

    @BeforeEach
    void setup() {
        service = new BenutzerService(repo, encoder);
        securityMock = mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void cleanup() {
        securityMock.close();
    }

    // ---------------------------------------------------------
    // createBenutzer
    // ---------------------------------------------------------

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

        var ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createBenutzer("mail@test.com", "testuser", "secret")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createUser_usernameTaken() {
        when(repo.existsByEmail("mail@test.com")).thenReturn(false);
        when(repo.existsByUsername("testuser")).thenReturn(true);

        var ex = assertThrows(
                ResponseStatusException.class,
                () -> service.createBenutzer("mail@test.com", "testuser", "secret")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    // ---------------------------------------------------------
    // getById
    // ---------------------------------------------------------

    @Test
    void getById_notFound() {
        when(repo.findById(5L)).thenReturn(Optional.empty());

        var ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getById(5)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getById_success() {
        Benutzer b = new Benutzer();
        b.setId(5L);

        when(repo.findById(5L)).thenReturn(Optional.of(b));

        var r = service.getById(5L);
        assertEquals(5L, r.getId());
    }

    // ---------------------------------------------------------
    // getByUsername
    // ---------------------------------------------------------

    @Test
    void getByUsername_notFound() {
        when(repo.findByUsername("abc")).thenReturn(Optional.empty());

        var ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getByUsername("abc")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getByUsername_success() {
        Benutzer b = new Benutzer();
        b.setUsername("abc");

        when(repo.findByUsername("abc")).thenReturn(Optional.of(b));

        assertEquals("abc", service.getByUsername("abc").getUsername());
    }

    // ---------------------------------------------------------
    // list
    // ---------------------------------------------------------

    @Test
    void list_returnsPage() {
        Benutzer b = new Benutzer();
        b.setId(1L);

        Page<Benutzer> page =
                new PageImpl<>(List.of(b), PageRequest.of(0, 10), 1);

        when(repo.findAll(any(PageRequest.class))).thenReturn(page);

        var result = service.list(0, 10);

        assertEquals(1, result.getTotalElements());
    }

    // ---------------------------------------------------------
    // getCurrentUser
    // ---------------------------------------------------------

    @Test
    void getCurrentUser_unauthorizedWhenNoAuth() {
        securityMock.when(SecurityContextHolder::getContext).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> service.getCurrentUser());
    }

    @Test
    void getCurrentUser_success() {
        SecurityContext ctx = mock(SecurityContext.class);
        TestingAuthenticationToken auth = new TestingAuthenticationToken("testuser", null);

        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
        when(ctx.getAuthentication()).thenReturn(auth);

        Benutzer b = new Benutzer();
        b.setUsername("testuser");

        when(repo.findByUsername("testuser")).thenReturn(Optional.of(b));

        var result = service.getCurrentUser();
        assertEquals("testuser", result.getUsername());
    }

    // ---------------------------------------------------------
    // patchBenutzerById
    // ---------------------------------------------------------

    @Test
    void patchBenutzerById_forbiddenIfDifferentUser() {
        Benutzer b = new Benutzer();
        b.setId(5L);
        b.setUsername("user1");

        when(repo.findById(5L)).thenReturn(Optional.of(b));

        // current user mock
        SecurityContext ctx = mock(SecurityContext.class);
        TestingAuthenticationToken auth = new TestingAuthenticationToken("user2", null);

        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
        when(ctx.getAuthentication()).thenReturn(auth);

        Benutzer b2 = new Benutzer();
        b2.setId(2L);
        when(repo.findByUsername("user2")).thenReturn(Optional.of(b2));

        assertThrows(ResponseStatusException.class,
                () -> service.patchBenutzerById(5L, "a", "b", "c"));
    }

    @Test
    void patchBenutzerById_success() {
        Benutzer existing = new Benutzer();
        existing.setId(5L);
        existing.setUsername("old");
        existing.setEmail("old@mail.com");

        when(repo.findById(5L)).thenReturn(Optional.of(existing));

        // Mock current user = same user
        SecurityContext ctx = mock(SecurityContext.class);
        TestingAuthenticationToken auth = new TestingAuthenticationToken("old", null);

        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
        when(ctx.getAuthentication()).thenReturn(auth);
        when(repo.findByUsername("old")).thenReturn(Optional.of(existing));

        when(encoder.encode("newPw")).thenReturn("ENC(newPw)");
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Benutzer result = service.patchBenutzerById(5L, "new@mail.com", "newuser", "newPw");

        assertEquals("new@mail.com", result.getEmail());
        assertEquals("newuser", result.getUsername());
        assertEquals("ENC(newPw)", result.getPasswordHash());
    }
}
