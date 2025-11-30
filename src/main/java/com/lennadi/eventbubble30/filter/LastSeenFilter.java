package com.lennadi.eventbubble30.filter;

import com.lennadi.eventbubble30.security.BenutzerDetails;
import com.lennadi.eventbubble30.features.service.BenutzerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Component
public class LastSeenFilter extends OncePerRequestFilter {

    @Autowired
    BenutzerService benutzerService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof BenutzerDetails details) {

            Long userId = details.getId();

            try {
                benutzerService.seen(userId);
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 404) {

                    HttpSession session = request.getSession(false);
                    if (session != null) session.invalidate();

                    SecurityContextHolder.clearContext();
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
