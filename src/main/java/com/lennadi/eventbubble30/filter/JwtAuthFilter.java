package com.lennadi.eventbubble30.filter;

import com.lennadi.eventbubble30.exceptions.ApiErrorResponse;
import com.lennadi.eventbubble30.security.BenutzerDetails;
import com.lennadi.eventbubble30.security.BenutzerDetailsService;
import com.lennadi.eventbubble30.security.token.JwtService;
import com.lennadi.eventbubble30.security.token.exceptions.TokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;
    private final BenutzerDetailsService benutzerDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        //log.warn("Authorization header = '{}'", authHeader);

        //if no token
        if (authHeader == null) {
            filterChain.doFilter(request,response);
            return;
        }

        //if wrong format
        String authPrefix = "Bearer ";
        if(!authHeader.startsWith(authPrefix)){
            log.warn("Wrong AuthHeader Format \"{}\", expecting \"Bearer <token>\"", authHeader);
            filterChain.doFilter(request,response);
            return;
        }

        String token = authHeader.substring(authPrefix.length());

        //if undefined token
        if (token.isEmpty() ||
                token.equalsIgnoreCase("null") ||
                token.equalsIgnoreCase("undefined")) {
            log.warn("Empty JWT");
            filterChain.doFilter(request, response);
            return;
        }

        try{
            Long userId = jwtService.extractUserId(token);
            BenutzerDetails user = (BenutzerDetails) benutzerDetailsService.loadUserById(userId);

            jwtService.validateAccessToken(token, user);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        }catch (TokenException e){//nix machen; als wäre da kein Token :)
            log.warn("Invalid JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            //throw new BadCredentialsException(e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected Error while resolving JWT: {}",e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
