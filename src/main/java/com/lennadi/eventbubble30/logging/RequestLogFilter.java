package com.lennadi.eventbubble30.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Duration GEO_TIMEOUT = Duration.ofSeconds(2);
    private static final int SEND_DELAY_SECONDS = 5;

    private static final Set<String> SUPPRESSED_PATH_SUBSTRINGS = Set.of(
            "/.env",
            "/.env.",
            "/config.yml",
            "/config.yaml",
            "/settings.yml",
            "/settings.yaml",
            "/phpinfo.php",
            "/.git/config",
            "/wp-config.php",
            "/server-status",
            "/actuator/env"
    );

    private static final Set<String> IGNORED_EXACT_PATHS = Set.of(
            "/favicon.ico",
            "/robots.txt",
            "/sitemap.xml",
            "/base.js",
            "/style.css",
            "/json",
            "/SDK/webLanguage",
            "/",
            "/ui"
    );

    private static final List<String> IGNORED_PATH_PREFIXES = List.of(
            "/wp-admin",
            "/wp-content",
            "/wp-includes",
            "/assets/",
            "/static/",
            "/css/",
            "/js/",
            "/images/",
            "/img/",
            "/webjars/"
    );

    private final TelegramNotifier telegramNotifier;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService lookupExecutor = Executors.newFixedThreadPool(4);
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(GEO_TIMEOUT)
            .build();

    private final List<Predicate<Bucket>> suppressors = List.of(
            this::isOnly404Noise,
            this::containsSuppressedProbe,
            this::containsOnlyIgnoredPaths,
            this::outsideGermany
    );

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
            b.locationFuture = CompletableFuture
                    .supplyAsync(() -> resolveLocation(ip), lookupExecutor)
                    .completeOnTimeout("-", GEO_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> "-");
            return b;
        });

        synchronized (bucket) {
            RequestEntry entry = new RequestEntry(
                    Instant.now(),
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getQueryString(),
                    response.getStatus()
            );
            bucket.entries.add(entry);

            if (bucket.future != null) {
                bucket.future.cancel(false);
            }

            bucket.future = scheduler.schedule(
                    () -> flush(bucketKey, bucket),
                    SEND_DELAY_SECONDS,
                    TimeUnit.SECONDS
            );
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
            if (bucket.entries.isEmpty()) {
                buckets.remove(bucketKey, bucket);
                return;
            }

            bucket.location = isBlank(resolvedLocation) ? "-" : resolvedLocation;

            if (telegramNotifier.getShouldFilterRequests() && shouldSuppress(bucket)) {
                bucket.entries.clear();
                bucket.future = null;
                buckets.remove(bucketKey, bucket);
                return;
            }

            String text = formatTelegramMessage(bucket);

            bucket.entries.clear();
            bucket.future = null;
            buckets.remove(bucketKey, bucket);

            telegramNotifier.send(text);
        }
    }

    private boolean shouldSuppress(Bucket bucket) {
        return suppressors.stream().anyMatch(rule -> rule.test(bucket));
    }

    private boolean isOnly404Noise(Bucket bucket) {
        return bucket.entries.size() > 1
                && bucket.entries.stream().allMatch(entry -> entry.status() == 404);
    }

    private boolean containsSuppressedProbe(Bucket bucket) {
        return bucket.entries.stream()
                .map(RequestEntry::fullPath)
                .map(String::toLowerCase)
                .anyMatch(path -> SUPPRESSED_PATH_SUBSTRINGS.stream().anyMatch(path::contains));
    }

    private boolean containsOnlyIgnoredPaths(Bucket bucket) {
        return !bucket.entries.isEmpty()
                && bucket.entries.stream()
                .map(RequestEntry::path)
                .allMatch(this::isIgnoredPath);
    }

    private boolean outsideGermany(Bucket bucket) {
        if(!bucket.locationFuture.isDone()){
            return false;
        }
        return !bucket.location.toLowerCase().contains("germany");
    }

    private boolean isIgnoredPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        if (IGNORED_EXACT_PATHS.contains(path)) {
            return true;
        }

        return IGNORED_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isStaticLookingPath(String path) {
        return path.startsWith("/assets/")
                || path.startsWith("/static/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/img/")
                || path.startsWith("/webjars/")
                || path.equals("/favicon.ico")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".gif")
                || path.endsWith(".svg")
                || path.endsWith(".webp")
                || path.endsWith(".ico")
                || path.endsWith(".map")
                || path.endsWith(".woff")
                || path.endsWith(".woff2")
                || path.endsWith(".ttf");
    }

    private String formatTelegramMessage(Bucket bucket) {
        StringBuilder sb = new StringBuilder();
        sb.append(bucket.ip)
                .append(" (")
                .append(bucket.location)
                .append(")\n")
                .append("UA: ")
                .append(bucket.userAgent)
                .append("\n\n");

        for (RequestEntry entry : bucket.entries) {
            sb.append(entry.timestamp())
                    .append(" | ")
                    .append(entry.method())
                    .append(" ")
                    .append(entry.fullPath())
                    .append(" | ")
                    .append(entry.status())
                    .append("\n");
        }

        return sb.toString().trim();
    }

    private String resolveLocation(String ip) {
        if (isBlank(ip) || isLocalOrPrivateIp(ip)) {
            return "-";
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ipwho.is/" + ip))
                    .timeout(GEO_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300 || isBlank(response.body())) {
                return "-";
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.path("success").asBoolean(false)) {
                return "-";
            }

            String country = root.path("country").asText("");
            String city = root.path("city").asText("");
            String region = root.path("region").asText("");

            if (!isBlank(country) && !isBlank(city)) {
                return country + ", " + city;
            }
            if (!isBlank(country) && !isBlank(region)) {
                return country + ", " + region;
            }
            if (!isBlank(country)) {
                return country;
            }

            return "-";
        } catch (Exception e) {
            return "-";
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!candidate.isBlank()) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isLocalOrPrivateIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress();
        } catch (Exception e) {
            return true;
        }
    }

    private String shorten(String s, int max) {
        if (s == null) return "-";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
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
        private final List<RequestEntry> entries = new ArrayList<>();
        private ScheduledFuture<?> future;
        private CompletableFuture<String> locationFuture;
    }

    private record RequestEntry(
            Instant timestamp,
            String method,
            String path,
            String query,
            int status
    ) {
        private String fullPath() {
            return query == null || query.isBlank() ? path : path + "?" + query;
        }
    }
}