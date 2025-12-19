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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
            @NotEmpty @NotBlank String name,
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

    @Audit(action = AuditLog.Action.CREATE, resourceType = AuditLog.RType.PROFILE, resourceIdExpression = "#result.body.id")
    @PostMapping("/{segment}")
    public ResponseEntity<Profil.DTO> createProfil(
            @PathVariable String segment,
            @Valid @RequestBody CreateProfilRequest request
    ) {
        long id = resolveId(segment);
        Profil created = profilService.createProfil(id, request);

        return ResponseEntity
                .created(URI.create("/api/profiles/" + id))
                .body(profilService.toDTO(created));
    }

    @GetMapping("/{segment}")
    public ResponseEntity<Profil.DTO> getProfil(@PathVariable String segment) {
        long id = resolveId(segment);
        Profil profil = profilService.getProfil(id);
        Profil.DTO ret = profilService.toDTO(profil);
        return ResponseEntity.ok(ret);
    }

    @Audit(action = AuditLog.Action.CREATE, resourceType = AuditLog.RType.PROFILE, resourceIdExpression = "#result.body.id")
    @PatchMapping("/{segment}")
    public ResponseEntity<Profil.DTO> updateProfil(
            @PathVariable String segment,
            @Valid @RequestBody UpdateProfilRequest request
    ) {
        long id = resolveId(segment);
        Profil updated = profilService.updateProfil(id, request);
        return ResponseEntity.ok(profilService.toDTO(updated));
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = AuditLog.RType.PROFILE, resourceIdExpression = "#request.getAttribute('auditResourceId')")    @DeleteMapping("/{segment}")
    public ResponseEntity<Void> deleteProfil(@PathVariable String segment) {
        long id = resolveId(segment);

        RequestContextHolder.currentRequestAttributes()
                .setAttribute("auditResourceId", id, RequestAttributes.SCOPE_REQUEST);

        profilService.deleteProfil(id);
        return ResponseEntity.noContent().build();
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = AuditLog.RType.PROFILE, resourceIdExpression = "#result.body.id")
    @PutMapping("/{segment}/avatar")
    public Profil.DTO uploadAvatar(
            @PathVariable String segment,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        long id = resolveId(segment);

        return profilService.toDTO(profilService.updateAvatar(id, file));
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = AuditLog.RType.PROFILE, resourceIdExpression = "#result.body.id")
    @DeleteMapping("/{segment}/avatar")
    public Profil.DTO deleteAvatar(@PathVariable String segment) {
        long id = resolveId(segment);

        return profilService.toDTO(profilService.deleteAvatar(id));//todo content removed response
    }

}
