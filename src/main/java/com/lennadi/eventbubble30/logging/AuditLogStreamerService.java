package com.lennadi.eventbubble30.logging;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class AuditLogStreamerService {
    private final Sinks.Many<AuditLog> sink =
            Sinks.many().multicast().onBackpressureBuffer();

    public void publish(AuditLog entry) {
        sink.tryEmitNext(entry);
    }

    public Flux<AuditLog> streamFiltered(
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

            if(resourceId != null && !resourceId.equals(log.getResourceId()))
                return false;

            if (success != null && !success.equals(log.isSuccess()))
                return false;

            return true;
        };

        return sink.asFlux().filter(filter);
    }
}
