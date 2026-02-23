package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.DTOLevel;
import com.lennadi.eventbubble30.features.IDTO;
import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Profil;
import com.lennadi.eventbubble30.features.service.DtoService;
import com.lennadi.eventbubble30.features.service.ProfilService;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfilController {

    private final ProfilService profilService;
    private final DtoService dtoService;

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

    private String resolveExtId(String segment) {
        if ("me".equalsIgnoreCase(segment)) {
            return profilService.getCurrentUserExternalId();
        }

        return segment;
        /*
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid identifier: " + segment);        }*/
    }

    // ----------------------------------------------------------
    // Endpoints (same methods for /me and /{id})
    // ----------------------------------------------------------

    @Audit(action = AuditLog.Action.CREATE, resourceType = EntityType.PROFILE, resourceIdExpression = "#result.body.id")
    @PostMapping("/{segment}")
    public ResponseEntity<IDTO> createProfil(
            @PathVariable String segment,
            @Valid @RequestBody CreateProfilRequest request
    ) {
        String extId = resolveExtId(segment);
        Profil created = profilService.createProfil(extId, request);

        return ResponseEntity
                .created(URI.create("/api/profiles/" + extId))
                .body(dtoService.get(created));
    }

    @GetMapping("/{segment}")
    public ResponseEntity<IDTO> getProfil(
            @PathVariable String segment,
            @RequestParam(defaultValue = "FULL") DTOLevel level
    ) throws BadRequestException {
        String extId = resolveExtId(segment);
        Profil profil = profilService.getProfil(extId);

        return ResponseEntity.ok(
                dtoService.get(profil, level)
        );
    }

    @Audit(action = AuditLog.Action.UPDATE, resourceType = EntityType.PROFILE, resourceIdExpression = "#result.body.id")
    @PatchMapping("/{segment}")
    public ResponseEntity<IDTO> updateProfil(
            @PathVariable String segment,
            @Valid @RequestBody UpdateProfilRequest request
    ) {
        String extId = resolveExtId(segment);
        Profil updated = profilService.updateProfil(extId, request);
        return ResponseEntity.ok(dtoService.get(updated));
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = EntityType.PROFILE, resourceIdExpression = "#request.getAttribute('auditResourceId')")
    @DeleteMapping("/{segment}")
    public ResponseEntity<Void> deleteProfil(@PathVariable String segment) {
        String extId = resolveExtId(segment);

        RequestContextHolder.currentRequestAttributes()
                .setAttribute("auditResourceId", extId, RequestAttributes.SCOPE_REQUEST);

        profilService.deleteProfil(extId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{segment}/exists")
    public boolean profileExists(@PathVariable String segment) {
        String extId = resolveExtId(segment);
        return profilService.exists(extId);
    }


    @Audit(action = AuditLog.Action.UPDATE, resourceType = EntityType.PROFILE, resourceIdExpression = "#result.body.id")
    @PutMapping(
            value = "/{segment}/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public IDTO uploadAvatar(
            @PathVariable String segment,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String extId = resolveExtId(segment);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }

        return dtoService.get(profilService.updateAvatar(extId, file));
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = EntityType.PROFILE, resourceIdExpression = "#request.getAttribute('auditResourceId')")
    @DeleteMapping("/{segment}/avatar")
    public ResponseEntity<Void> deleteAvatar(@PathVariable String segment) {
        String extId = resolveExtId(segment);
        RequestContextHolder.currentRequestAttributes()
                .setAttribute("auditResourceId", extId, RequestAttributes.SCOPE_REQUEST);

        profilService.deleteAvatar(extId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("{segment}/avatar")
    public ResponseEntity<URI> getAvatar(@PathVariable String segment) {
        String extId = resolveExtId(segment);

        URL url = profilService.getAvatarUrl(extId);
        if (url == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(URI.create(url.toString()));
    }

    //todo events // was? (mby events von profil?)

}
