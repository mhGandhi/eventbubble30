package com.lennadi.eventbubble30.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogStreamerService {
    private final Sinks.Many<AuditLog> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(AuditLog entry) {
        var result = sink.tryEmitNext(entry);
        if (result.isFailure()) {
            log.warn("SSE emit failed: {}", result);
        }

    }

    public void registerListener(
            SseEmitter emitter,
            Long userId,
            List<AuditLog.Action> action,
            AuditLog.RType resourceType,
            Long resourceId,
            Boolean success
    ) {
        Predicate<AuditLog> filter = log -> {

            if (userId != null && (log.getBenutzer() == null ||
                    !userId.equals(log.getBenutzer().getId())))
                return false;

            if (action != null && !action.isEmpty() &&
                    !action.contains(log.getAction()))
                return false;

            if (resourceType != null &&
                    !resourceType.equals(log.getResourceType()))
                return false;

            if (resourceId != null &&
                    !resourceId.equals(log.getResourceId()))
                return false;

            if (success != null && !success.equals(log.isSuccess()))
                return false;

            return true;
        };

        Listener listener = new Listener(emitter, filter);
        listeners.add(listener);

        emitter.onCompletion(() -> listeners.remove(listener));
        emitter.onTimeout(() -> listeners.remove(listener));
        emitter.onError(e -> listeners.remove(listener));
    }

    private record Listener(SseEmitter emitter, Predicate<AuditLog> filter) {}
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
}
