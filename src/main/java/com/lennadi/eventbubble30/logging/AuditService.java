package com.lennadi.eventbubble30.logging;

import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditRepository;
    private final AuditLogStreamerService auditLogStreamerService;

    public void log(
            Benutzer benutzer,
            String ip,
            String usernameSnapshot,
            Set<Benutzer.Role> roleSnapshot,
            AuditLog.Action action,
            String payload,
            boolean success,
            String endpoint,
            AuditLog.RType resourceType,
            Long resourceId
    ) {
        AuditLog log = new AuditLog(
                benutzer,
                ip,
                usernameSnapshot,
                roleSnapshot,
                action,
                payload,
                success,
                endpoint,
                Instant.now(),
                resourceType,
                resourceId
        );

        auditRepository.save(log);
        auditLogStreamerService.publish(log);
    }

    public void logSystemAction(
            AuditLog.Action action,
            String message,
            boolean success,
            String endpoint,
            AuditLog.RType resourceType,
            Long resourceId
    ) {
        log(
                null, // no user
                "127.0.0.1", // or "SYSTEM"
                "SYSTEM",
                Set.of(), // empty roles
                action,
                message,
                success,
                endpoint,
                resourceType,
                resourceId
        );
    }

}
