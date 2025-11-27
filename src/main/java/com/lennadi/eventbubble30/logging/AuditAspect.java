package com.lennadi.eventbubble30.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lennadi.eventbubble30.entities.Benutzer;
import com.lennadi.eventbubble30.logging.Audit;
import com.lennadi.eventbubble30.service.BenutzerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

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

    @Around("@annotation(audit)")
    public Object auditAction(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        boolean success = false;
        Object result = null;
        Exception thrown = null;

        try {
            result = pjp.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            success = false;
            thrown = e;
            throw e;
        } finally {
            writeAuditLog(pjp, audit, success, thrown);
        }
    }

    private void writeAuditLog(
            ProceedingJoinPoint pjp, Audit audit, boolean success, Exception ex) {

        Benutzer user = safeGetUser();

        Long resourceId = extractResourceId(pjp, audit.resourceIdParam());
        String payload = serializeArgs(pjp.getArgs());

        AuditLog log = new AuditLog(
                user,
                getClientIp(),
                user!=null?user.getUsername():null,
                user!=null?Set.copyOf(user.getRoles()):Set.of(),
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

    private Long extractResourceId(ProceedingJoinPoint pjp, String name) {
        if (name.isBlank()) return 0L;

        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String[] params = sig.getParameterNames();
        Object[] args = pjp.getArgs();

        for (int i = 0; i < params.length; i++) {
            if (params[i].equals(name)) {
                Object a = args[i];
                if (a instanceof Long l) return l;
                if (a instanceof Number n) return n.longValue();
            }
        }
        return 0L;
    }

    // ====================================================================
    //                      SENSITIVE DATA MASKING
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

    private String getClientIp() {
        return request.getHeader("X-Forwarded-For") != null
                ? request.getHeader("X-Forwarded-For")
                : request.getRemoteAddr();
    }

    private Benutzer safeGetUser() {
        try {
            return benutzerService.getCurrentUser();
        } catch (Exception ex) {
            return null; // anonymous
        }
    }

}
