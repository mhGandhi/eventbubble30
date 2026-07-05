package com.lennadi.eventbubble30.logging;

import com.lennadi.eventbubble30.features.service.TelegramNotifier;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class RequestLogFilter extends OncePerRequestFilter {

    private final TelegramNotifier telegramNotifier;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        String ip = extractClientIp(request);
        String userAgent = shorten(request.getHeader("User-Agent"), 120);
        String bucketKey = ip + "|" + userAgent;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> new Bucket());

        synchronized (bucket) {
            if (bucket.ip == null) {
                bucket.ip = ip;
            }
            if (bucket.userAgent == null) {
                bucket.userAgent = userAgent;
            }

            bucket.lines.add(String.format(
                    "%s | %s %s | %d",
                    Instant.now(),
                    request.getMethod(),
                    request.getRequestURI() + (request.getQueryString() != null ? "?" + request.getQueryString() : ""),
                    response.getStatus()
            ));

            if (bucket.future != null) {
                bucket.future.cancel(false);
            }

            bucket.future = scheduler.schedule(() -> flush(bucketKey, bucket), 3, TimeUnit.SECONDS);
        }
    }

    private void flush(String bucketKey, Bucket bucket) {
        synchronized (bucket) {
            if (bucket.lines.isEmpty()) {
                buckets.remove(bucketKey, bucket);
                return;
            }

            String text = """
                    Requests from %s
                    UA: %s

                    %s
                    """.formatted(
                    bucket.ip,
                    bucket.userAgent,
                    String.join("\n", bucket.lines)
            );

            bucket.lines.clear();
            bucket.future = null;
            buckets.remove(bucketKey, bucket);

            telegramNotifier.send(text);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String shorten(String s, int max) {
        if (s == null) return "-";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
    }

    private static class Bucket {
        private String ip;
        private String userAgent;
        private final List<String> lines = new ArrayList<>();
        private ScheduledFuture<?> future;
    }
}