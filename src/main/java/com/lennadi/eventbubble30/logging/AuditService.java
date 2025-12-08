package com.lennadi.eventbubble30.logging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditRepository;

    public void logSystemAction(
            AuditLog.Action action,
            String message,
            boolean success,
            String endpoint,
            AuditLog.RType resourceType,
            Long resourceId
    ) {
        AuditLog log = new AuditLog(
                null, // no user
                "127.0.0.1", // or "SYSTEM"
                "SYSTEM",
                Set.of(), // empty roles
                action,
                message,
                success,
                endpoint,
                Instant.now(),
                resourceType,
                resourceId
        );

        auditRepository.save(log);
    }

}
