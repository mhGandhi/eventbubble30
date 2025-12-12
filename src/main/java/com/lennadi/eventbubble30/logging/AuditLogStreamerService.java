package com.lennadi.eventbubble30.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
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

            if (resourceType != null && !resourceType.equals(log.getResourceType()))
                return false;

            if (resourceId != null && !resourceId.equals(log.getResourceId()))
                return false;

            if (success != null && !success.equals(log.isSuccess()))
                return false;

            return true;
        };

        AtomicReference<Disposable> subRef = new AtomicReference<>();

        Disposable subscription = sink.asFlux()
                .filter(filter)
                .subscribe(
                        logEntry -> {
                            try {
                                emitter.send(SseEmitter.event().data(logEntry.toDTO()));
                            } catch (Exception e) {
                                subRef.get().dispose();
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            subRef.get().dispose();
                            emitter.completeWithError(error);
                        },
                        () -> {
                            subRef.get().dispose();
                            emitter.complete();
                        }
                );

        // save into reference AFTER subscription created
        subRef.set(subscription);

        emitter.onCompletion(() -> subRef.get().dispose());
        emitter.onTimeout(() -> subRef.get().dispose());
        emitter.onError(e -> subRef.get().dispose());
    }


}
