package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.db.entities.Profil;
import com.lennadi.eventbubble30.features.service.ProfilService;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfilController {

    private final ProfilService profilService;

    // ----------------------------------------------------------
    // DTOs
    // ----------------------------------------------------------

    public record CreateProfilRequest(
            @NotEmpty String name,
            String bio
    ) {}

    public record UpdateProfilRequest(
            @NotBlank String name,
            String bio
    ) {}

    // ----------------------------------------------------------
    // Internal ID resolver
    // ----------------------------------------------------------

    private long resolveId(String segment) {
        if ("me".equalsIgnoreCase(segment)) {
            return profilService.getCurrentUserId();
        }

        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid identifier: " + segment);        }
    }

    // ----------------------------------------------------------
    // Endpoints (same methods for /me and /{id})
    // ----------------------------------------------------------

    @Audit(action = AuditLog.Action.CREATE, resourceType = "Profil", resourceIdExpression = "#result.body.id")
    @PostMapping("/{segment}")
    public ResponseEntity<Profil.DTO> createProfil(
            @PathVariable String segment,
            @Valid @RequestBody CreateProfilRequest request
    ) {
        long id = resolveId(segment);
        Profil created = profilService.createProfil(id, request);

        return ResponseEntity
                .created(URI.create("/api/profiles/" + id))
                .body(created.toDTO());
    }

    @GetMapping("/{segment}")
    public ResponseEntity<Profil.DTO> getProfil(@PathVariable String segment) {
        long id = resolveId(segment);
        Profil profil = profilService.getProfil(id);
        return ResponseEntity.ok(profil.toDTO());
    }

    @Audit(action = AuditLog.Action.CREATE, resourceType = "Profil", resourceIdExpression = "#result.body.id")
    @PatchMapping("/{segment}")
    public ResponseEntity<Profil.DTO> updateProfil(
            @PathVariable String segment,
            @Valid @RequestBody UpdateProfilRequest request
    ) {
        long id = resolveId(segment);
        Profil updated = profilService.updateProfil(id, request);
        return ResponseEntity.ok(updated.toDTO());
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = "Profil", resourceIdExpression = "#request.getAttribute('auditResourceId')")    @DeleteMapping("/{segment}")
    public ResponseEntity<Void> deleteProfil(@PathVariable String segment) {
        long id = resolveId(segment);

        RequestContextHolder.currentRequestAttributes()
                .setAttribute("auditResourceId", id, RequestAttributes.SCOPE_REQUEST);

        profilService.deleteProfil(id);
        return ResponseEntity.noContent().build();
    }
}
