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
        try{
            var auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()
                    && auth.getPrincipal() instanceof BenutzerDetails details) {

                Long userId = details.getId();

                try {
                    benutzerService.seen(userId);
                } catch (RuntimeException ex) {
                    log.warn("Error while updating seen for user {}: {}", userId, ex.getMessage());
                }
            }
        }catch (Exception ex){
            log.error("Unexpected error in LastSeenFilter: {}", ex.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
