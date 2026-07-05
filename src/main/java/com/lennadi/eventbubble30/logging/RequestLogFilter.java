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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
public class RequestLogFilter extends OncePerRequestFilter {

    private final TelegramNotifier telegramNotifier;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService lookupExecutor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        String ip = extractClientIp(request);
        String userAgent = shorten(request.getHeader("User-Agent"), 300);
        String bucketKey = ip + "|" + userAgent;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> {
            Bucket b = new Bucket();
            b.ip = ip;
            b.userAgent = userAgent;
            b.locationFuture = CompletableFuture.supplyAsync(() -> resolveLocation(ip), lookupExecutor)
                    .completeOnTimeout("-", 2, TimeUnit.SECONDS)
                    .exceptionally(ex -> "-");
            return b;
        });

        if(response.getStatus()==404)return; //no 404s

        synchronized (bucket) {
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
        String resolvedLocation;
        try {
            resolvedLocation = bucket.locationFuture != null ? bucket.locationFuture.get() : "-";
        } catch (Exception e) {
            resolvedLocation = "-";
        }

        synchronized (bucket) {
            if (bucket.lines.isEmpty()) {
                buckets.remove(bucketKey, bucket);
                return;
            }

            bucket.location = resolvedLocation != null && !resolvedLocation.isBlank() ? resolvedLocation : "-";

            String text = """
                    %s (%s)
                    UA: %s

                    %s
                    """.formatted(
                    bucket.ip,
                    bucket.location,
                    bucket.userAgent,
                    String.join("\n", bucket.lines)
            );

            bucket.lines.clear();
            bucket.future = null;
            buckets.remove(bucketKey, bucket);

            telegramNotifier.send(text);
        }
    }

    private String resolveLocation(String ip) {
        if (ip == null || ip.isBlank() || isLocalIp(ip)) {
            return "-";
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ipwho.is/" + ip))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (body == null || body.isBlank()) {
                return "-";
            }

            String success = extractJsonString(body, "\"success\":", ",", "}");
            if (success != null && success.trim().startsWith("false")) {
                return "-";
            }

            String country = extractJsonString(body, "\"country\":\"", "\"");
            String city = extractJsonString(body, "\"city\":\"", "\"");
            String region = extractJsonString(body, "\"region\":\"", "\"");

            if (country == null || country.isBlank()) {
                return "-";
            }

            if (city != null && !city.isBlank()) {
                return country + ", " + city;
            }

            if (region != null && !region.isBlank()) {
                return country + ", " + region;
            }

            return country;
        } catch (Exception e) {
            return "-";
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

    private boolean isLocalIp(String ip) {
        return ip.startsWith("127.")
                || ip.equals("::1")
                || ip.equals("0:0:0:0:0:0:0:1")
                || ip.startsWith("192.168.")
                || ip.startsWith("10.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.20.")
                || ip.startsWith("172.21.")
                || ip.startsWith("172.22.")
                || ip.startsWith("172.23.")
                || ip.startsWith("172.24.")
                || ip.startsWith("172.25.")
                || ip.startsWith("172.26.")
                || ip.startsWith("172.27.")
                || ip.startsWith("172.28.")
                || ip.startsWith("172.29.")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.");
    }

    private String extractJsonString(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        if (start < 0) return null;
        start += startToken.length();

        int end = source.indexOf(endToken, start);
        if (end < 0) return null;

        return source.substring(start, end)
                .replace("\\/", "/")
                .replace("\\\"", "\"");
    }

    private String extractJsonString(String source, String startToken, String endToken1, String endToken2) {
        int start = source.indexOf(startToken);
        if (start < 0) return null;
        start += startToken.length();

        int end1 = source.indexOf(endToken1, start);
        int end2 = source.indexOf(endToken2, start);

        int end;
        if (end1 < 0) end = end2;
        else if (end2 < 0) end = end1;
        else end = Math.min(end1, end2);

        if (end < 0) return null;
        return source.substring(start, end);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        lookupExecutor.shutdown();
    }

    private static class Bucket {
        private String ip;
        private String location = "-";
        private String userAgent;
        private final List<String> lines = new ArrayList<>();
        private ScheduledFuture<?> future;
        private CompletableFuture<String> locationFuture;
    }
}