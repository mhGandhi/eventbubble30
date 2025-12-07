package com.lennadi.eventbubble30.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lennadi.eventbubble30.features.db.entities.Benutzer;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository repo;
    private final BenutzerService benutzerService;
    private final HttpServletRequest request;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String MASK = "***REDACTED***";

    // ====================================================================
    //                            MAIN AROUND ADVICE
    // ====================================================================

    @Around("@annotation(audit)")
    public Object auditAction(ProceedingJoinPoint pjp, Audit audit) throws Throwable {

        Object result = null;
        Exception thrown = null;

        try {
            result = pjp.proceed();
            return result;
        } catch (Exception e) {
            thrown = e;
            throw e; // DO NOT swallow business exceptions
        } finally {
            try {
                boolean success = (thrown == null);

                Long resourceId = null;

                // 1) priority: explicit callback param
                resourceId = extractResourceIdParam(pjp, audit.resourceIdParam());

                // 2) fallback: extract from result
                if (resourceId == null) resourceId = extractFromResult(result);

                // 3) fallback: extract from exception object
                if (resourceId == null) resourceId = extractFromException(thrown);

                // 4) fallback: actions that modify current user
                if (resourceId == null && modifiesCurrentUser(audit.action())) {
                    Benutzer current = safeGetUser();
                    resourceId = current != null ? current.getId() : null;
                }

                writeAuditLogSafe(pjp, audit, success, resourceId);

            } catch (Exception e) {
                // FULLY swallow audit failures
                log.error("Audit logging failed (swallowed): {}", e.getMessage(), e);
            }
        }
    }

    // ====================================================================
    //           SAFE WRAPPER FOR THE ACTUAL PERSIST OPERATION
    // ====================================================================

    private void writeAuditLogSafe(
            ProceedingJoinPoint pjp,
            Audit audit,
            boolean success,
            Long resourceId
    ) {
        try {
            writeAuditLog(pjp, audit, success, resourceId);
        } catch (Exception e) {
            log.error("Audit log DB insert failed (swallowed): {}", e.getMessage(), e);
        }
    }

    // ====================================================================
    //                         LOG CREATION
    // ====================================================================

    private void writeAuditLog(
            ProceedingJoinPoint pjp,
            Audit audit,
            boolean success,
            Long resourceId
    ) {

        Benutzer user = safeGetUser();
        String payload = serializeArgs(pjp.getArgs());

        AuditLog log = new AuditLog(
                user,
                getClientIp(),
                user != null ? user.getUsername() : null,
                user != null ? Set.copyOf(user.getRoles()) : Set.of(),
                audit.action(),
                payload,
                success,
                request.getRequestURI(),
                Instant.now(),
                audit.resourceType(),
                resourceId
        );

        repo.save(log);
    }

    // ====================================================================
    //                  RESOURCE ID EXTRACTION (PARAMETER)
    // ====================================================================

    private Long extractResourceIdParam(ProceedingJoinPoint pjp, String paramName) {
        if (paramName == null || paramName.isBlank()) return null;

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] params = sig.getParameterNames();
        Object[] args = pjp.getArgs();

        for (int i = 0; i < params.length; i++) {
            if (params[i].equals(paramName)) {
                return extractIdFromObject(args[i]);
            }
        }
        return null;
    }

    // ====================================================================
    //                     RESOURCE ID EXTRACTION (RESULT)
    // ====================================================================

    private Long extractFromResult(Object result) {
        if (result == null) return null;

        if (result instanceof ResponseEntity<?> r) {
            return extractIdFromObject(r.getBody());
        }

        return extractIdFromObject(result);
    }

    // ====================================================================
    //            RESOURCE ID EXTRACTION (EXCEPTION OBJECTS)
    // ====================================================================

    private Long extractFromException(Exception ex) {
        if (ex == null) return null;
        return extractIdFromObject(ex);
    }

    // ====================================================================
    //                     UNIVERSAL ID EXTRACTOR
    // ====================================================================

    private Long extractIdFromObject(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Benutzer b) return b.getId();
        if (obj instanceof Benutzer.DTO dto) return dto.id();

        try {
            Field f = obj.getClass().getDeclaredField("id");
            f.setAccessible(true);
            Object val = f.get(obj);
            if (val instanceof Number num) return num.longValue();
        } catch (Exception ignore) {}

        return null;
    }

    // ====================================================================
    //                   ACTIONS THAT MODIFY CURRENT USER
    // ====================================================================

    private boolean modifiesCurrentUser(AuditLog.Action action) {
        return action == AuditLog.Action.LOGIN
                || action == AuditLog.Action.REFRESH
                || action == AuditLog.Action.INVALIDATE_TOKENS
                || action == AuditLog.Action.UPDATE;
    }

    // ====================================================================
    //                       SENSITIVE DATA MASKING
    // ====================================================================

    private String serializeArgs(Object[] args) {
        try {
            String json = mapper.writeValueAsString(args);
            JsonNode root = mapper.readTree(json);
            maskSensitiveFields(root);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return Arrays.toString(args);
        }
    }

    private void maskSensitiveFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(field -> {
                JsonNode child = obj.get(field);
                if (isSensitiveKey(field)) {
                    obj.put(field, MASK);
                } else {
                    maskSensitiveFields(child);
                }
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskSensitiveFields(child);
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        return switch (key) {
            case "password", "oldPassword", "newPassword",
                 "accessToken", "refreshToken" -> true;
            default -> {
                String lower = key.toLowerCase();
                yield lower.contains("password") || lower.contains("token");
            }
        };
    }

    // ====================================================================
    //                             HELPERS
    // ====================================================================

    private String getClientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded : request.getRemoteAddr();
    }

    private Benutzer safeGetUser() {
        try {
            return benutzerService.getCurrentUser();
        } catch (Exception ex) {
            return null;
        }
    }
}
