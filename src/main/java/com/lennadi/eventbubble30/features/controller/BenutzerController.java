package com.lennadi.eventbubble30.features.controller;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static com.lennadi.eventbubble30.logging.AuditLog.Action.UPDATE;

@CrossOrigin(origins = "*")
@RequestMapping("/api/user")
@RestController
@RequiredArgsConstructor
public class BenutzerController {

    private final BenutzerService service;

    private long resolveId(String segment) {
        if ("me".equalsIgnoreCase(segment)) {
            return service.getCurrentUser().getId();
        }

        try {
            return Long.parseLong(segment);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid identifier: " + segment);
        }
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

    @Audit(action = AuditLog.Action.CREATE, resourceType = AuditLog.RType.USER, resourceIdExpression = "#result.body.id")
    @PostMapping("/create")
    public ResponseEntity<Benutzer.DTO> createUser(@Valid @RequestBody CreateBenutzerRequest req) {

        Benutzer neu = service.createBenutzer(req);

        return ResponseEntity
                .created(URI.create("/api/user/" + neu.getId())) // Location Header
                .body(neu.toDTO());                                     // Response Body
    }

    @Audit(action = UPDATE, resourceType = AuditLog.RType.USER, resourceIdExpression = "#result.body.id")
    @PatchMapping("/{segment}")
    public ResponseEntity<Benutzer.DTO> patchUser(
            @PathVariable String segment,
            @Valid @RequestBody PatchBenutzerRequest req
    ) {
        long id = resolveId(segment);

        Benutzer b = service.updateBenutzer(id, req);

        return ResponseEntity
                .ok()
                .body(b.toDTO());
    }

    @Audit(action = AuditLog.Action.DELETE, resourceType = AuditLog.RType.USER, resourceIdExpression = "#request.getAttribute('auditResourceId')")
    @DeleteMapping("/{segment}")
    public ResponseEntity<Void> deleteUserById(@PathVariable String segment) {
        long id = resolveId(segment);

        RequestContextHolder.currentRequestAttributes()
                .setAttribute("auditResourceId", id, RequestAttributes.SCOPE_REQUEST);

        service.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{segment}")
    public Benutzer.DTO findUser(@PathVariable String segment) {
        long id = resolveId(segment);
        Benutzer ret = service.getBenutzer(id);

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
            resourceType = AuditLog.RType.USER,
            resourceIdExpression = "#currentUser.id"
    )
    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody UpdateOwnPasswordRequest req
    ) {
        Benutzer benutzer = service.getCurrentUser();
        service.changePassword(benutzer.getId(), req.oldPassword, req.newPassword);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public int getUserCount() {
        return service.getUserCount();
    }

}
