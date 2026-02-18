package com.lennadi.eventbubble30.logging;

import com.lennadi.eventbubble30.features.db.EntityType;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditRepository;
    private final AuditLogStreamerService auditLogStreamerService;
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuditService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            Benutzer benutzer,
            String ip,
            String usernameSnapshot,
            Set<Benutzer.Role> roleSnapshot,
            AuditLog.Action action,
            String payload,
            boolean success,
            String endpoint,
            EntityType resourceType,
            String resourceId
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

        try {
            auditLogStreamerService.publish(log);
        } catch (Exception e) {
            LOGGER.warn("Audit stream failed (ignored): {}", e.getMessage());
        }
    }

    public void logSystemAction(
            AuditLog.Action action,
            String message,
            boolean success,
            String endpoint,
            EntityType resourceType,
            String resourceId
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
