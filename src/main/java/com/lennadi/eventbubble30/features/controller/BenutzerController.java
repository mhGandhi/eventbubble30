package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.net.URI;

import static com.lennadi.eventbubble30.logging.AuditLog.Action.UPDATE;

@RequestMapping("/api/user")
@RestController
@RequiredArgsConstructor
public class BenutzerController {

    private final BenutzerService service;

    private String resolveExtId(String segment) {
        if ("me".equalsIgnoreCase(segment)) {
            return service.getCurrentUser().getExternalId();
        }
        return segment;
        /*
        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid identifier: " + segment);
        }*/
    }

    // ===== DTOs =====

    public record CreateBenutzerRequest(
            @Email @NotEmpty String email,
            @NotEmpty @Size(min = 3, max = 20)
            @Pattern(regexp = "^[a-zA-Z0-9_]+$")
            String username,
            @NotEmpty @Size(min = 8, max = 20)
            String password
    ) {}

    public record PatchBenutzerRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 3, max = 20)
            @Pattern(regexp = "^[a-zA-Z0-9_]+$")
            String username
    ) {}

    public record UpdateOwnPasswordRequest(
            @NotEmpty
            String oldPassword,
            @NotEmpty @Size(min = 8, max = 20)
            String newPassword
    ) {}

    // ===== Endpoints =====

    @Audit(action = AuditLog.Action.CREATE, resourceType = EntityType.USER, resourceIdExpression = "#result.body.id")
    @PostMapping("/create")
    public ResponseEntity<Benutzer.DTO> createUser(@Valid @RequestBody CreateBenutzerRequest req) {

        Benutzer neu = service.createBenutzer(req);

        return ResponseEntity
                .created(URI.create("/api/user/" + neu.getExternalId())) // Location Header
                .body(neu.toDTO());                                     // Response Body
    }

    @Audit(action = UPDATE, resourceType = EntityType.USER, resourceIdExpression = "#result.body.id")
    @PatchMapping("/{segment}")
    public ResponseEntity<Benutzer.DTO> patchUser(
            @PathVariable String segment,
            @Valid @RequestBody PatchBenutzerRequest req
    ) {
        String extId = resolveExtId(segment);

        Benutzer b = service.updateBenutzer(extId, req);

        return ResponseEntity
                .ok()
                .body(b.toDTO());
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = EntityType.USER, resourceIdExpression = "#request.getAttribute('auditResourceId')")
    @DeleteMapping("/{segment}")
    public ResponseEntity<Void> deleteUserById(@PathVariable String segment) {
        String extId = resolveExtId(segment);

        RequestContextHolder.currentRequestAttributes()
                .setAttribute("auditResourceId", extId, RequestAttributes.SCOPE_REQUEST);

        service.deleteUserById(extId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{segment}")
    public Benutzer.DTO findUser(@PathVariable String segment) {
        String extId = resolveExtId(segment);
        Benutzer ret = service.getBenutzer(extId);

        return ret.toDTO();
    }

    @GetMapping({"", "/"})
    public Page<Benutzer.DTO> listUsers(//todo pages
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.list(page, size).map(Benutzer::toDTO);
    }

    @Audit(
            action = UPDATE,
            resourceType = EntityType.USER,
            resourceIdExpression = "#currentUser.externalId"
    )
    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody UpdateOwnPasswordRequest req
    ) {
        Benutzer benutzer = service.getCurrentUser();
        service.changePassword(benutzer.getExternalId(), req.oldPassword, req.newPassword);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public int getUserCount() {
        return service.getUserCount();
    }

}
