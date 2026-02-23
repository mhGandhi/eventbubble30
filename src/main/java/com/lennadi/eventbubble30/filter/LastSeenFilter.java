package com.lennadi.eventbubble30.filter;

import com.lennadi.eventbubble30.security.BenutzerDetails;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LastSeenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LastSeenFilter.class);
    private final BenutzerService benutzerService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = getClientIp(request);
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof BenutzerDetails details) {

                Long userId = details.getId();
                log.info("[Anfrage] {}@{}: {}", auth.getName(), ip, request.getRequestURI());

                try {
                    benutzerService.seen(userId);
                } catch (RuntimeException ex) {
                    log.warn("Error while updating seen for user {}: {}", userId, ex.getMessage());
                }

            } else {
                log.info("[Anfrage] ?@{}: {}", ip, request.getRequestURI());
            }

        } catch (Exception ex) {
            log.error("Unexpected error in LastSeenFilter: {}", ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }

}
