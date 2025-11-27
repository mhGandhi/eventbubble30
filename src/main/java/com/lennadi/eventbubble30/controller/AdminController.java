package com.lennadi.eventbubble30.controller;

import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.logging.AuditLog;
import com.lennadi.eventbubble30.logging.AuditLogRepository;
import com.lennadi.eventbubble30.repository.ServerConfigRepository;
import com.lennadi.eventbubble30.service.ServerConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AuditLogRepository auditLogRepository;
    private final ServerConfigService serverConfigService;


    @GetMapping("/audit-log")
    public Page<AuditLog.DTO> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,

            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) List<AuditLog.Action> action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to
    ) {
        var pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        // collect dynamic filters
        List<Specification<AuditLog>> filters = new java.util.ArrayList<>();

        if (userId != null) {
            filters.add((root, q, cb) ->
                    cb.equal(root.get("benutzer").get("id"), userId));
        }

        if (action != null && !action.isEmpty()) {
            filters.add((root, q, cb) ->
                    root.get("action").in(action));
        }

        if (resourceType != null) {
            filters.add((root, q, cb) ->
                    cb.equal(root.get("resourceType"), resourceType));
        }

        if (success != null) {
            filters.add((root, q, cb) ->
                    cb.equal(root.get("success"), success));
        }

        if (from != null) {
            filters.add((root, q, cb) ->
                    cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }

        if (to != null) {
            filters.add((root, q, cb) ->
                    cb.lessThanOrEqualTo(root.get("timestamp"), to));
        }

        Specification<AuditLog> finalSpec =
                filters.isEmpty()
                        ? Specification.allOf()
                        : Specification.allOf(filters);

        return auditLogRepository
                .findAll(finalSpec, pageable)
                .map(AuditLog::toDTO);

    }

    @Audit(action = AuditLog.Action.INVALIDATE_TOKENS, resourceType = "ServerConfig")
    @PostMapping("invalidate-tokens")
    public ResponseEntity<Void> invalidateTokens() {
        try{
            serverConfigService.invalidateAllTokensNow();
            return ResponseEntity.noContent().build();
        }catch(Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

}
