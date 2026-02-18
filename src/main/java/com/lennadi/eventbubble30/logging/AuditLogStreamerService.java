package com.lennadi.eventbubble30.logging;

import com.lennadi.eventbubble30.features.db.EntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogStreamerService {

    /**
     * Best-effort multicast:
     * - no unbounded buffering
     * - slow clients drop events instead of OOM
     */
    private final Sinks.Many<AuditLog> sink =
            Sinks.many().multicast().directBestEffort();

    public void publish(AuditLog entry) {
        sink.emitNext(entry, (signalType, emitResult) -> {
            if (emitResult.isFailure()) {
                log.warn("SSE emit failed: {}", emitResult);
            }
            return false;
        });
    }

    public void registerListener(
            SseEmitter emitter,
            String userId,
            List<AuditLog.Action> actions,
            EntityType resourceType,
            String resourceId,
            Boolean success
    ) {

        Predicate<AuditLog> filter = log -> {

            if (userId != null &&
                    (log.getBenutzer() == null ||
                            !userId.equals(log.getBenutzer().getExternalId())))
                return false;

            if (actions != null && !actions.isEmpty() &&
                    !actions.contains(log.getAction()))
                return false;

            if (resourceType != null &&
                    !resourceType.equals(log.getResourceType()))
                return false;

            if (resourceId != null &&
                    !resourceId.equals(log.getResourceId()))
                return false;

            if (success != null &&
                    !success.equals(log.isSuccess()))
                return false;

            return true;
        };

        final Disposable[] subscriptionRef = new Disposable[1];

        subscriptionRef[0] = sink.asFlux()
                .filter(filter)
                .publishOn(Schedulers.single())
                .subscribe(
                        logEntry -> {
                            try {
                                emitter.send(
                                        SseEmitter.event()
                                                .name("audit-log")
                                                .data(logEntry.toDTO())
                                );
                            } catch (Exception e) {
                                log.debug("SSE client disconnected", e);
                                subscriptionRef[0].dispose();
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.warn("SSE stream error", error);
                            subscriptionRef[0].dispose();
                            emitter.completeWithError(error);
                        },
                        () -> {
                            subscriptionRef[0].dispose();
                            emitter.complete();
                        }
                );


        emitter.onCompletion(() -> subscriptionRef[0].dispose());
        emitter.onTimeout(() -> {
            subscriptionRef[0].dispose();
            emitter.complete();
        });
        emitter.onError(e -> subscriptionRef[0].dispose());
    }
}
