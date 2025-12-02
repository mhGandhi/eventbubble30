package com.lennadi.eventbubble30.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lennadi.eventbubble30.exceptions.ApiErrorResponse;
import com.lennadi.eventbubble30.exceptions.ForbiddenException;
import com.lennadi.eventbubble30.exceptions.UnauthorizedException;
import com.lennadi.eventbubble30.filter.JwtAuthFilter;
import com.lennadi.eventbubble30.filter.LastSeenFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final BenutzerDetailsService benutzerDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, LastSeenFilter lastSeenFilter) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        //static (kommt dann weg iwann) todo
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/apiDoc.yml").permitAll()

                        //admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        //auth
                        .requestMatchers("/api/auth/**").permitAll()

                        //user
                        .requestMatchers("/api/user/**").authenticated()

                        //events
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers("/api/events/**").authenticated()

                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::handleAuthError)
                        .accessDeniedHandler(this::handleAccessDenied)
                )

                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(lastSeenFilter, JwtAuthFilter.class);
                ;

        return http.build();
    }

    private void handleAuthError(HttpServletRequest request,
                                 HttpServletResponse response,
                                 AuthenticationException ex) throws IOException {

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                401,
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        writeJson(response, 401, body);
    }

    private void handleAccessDenied(HttpServletRequest request,
                                    HttpServletResponse response,
                                    AccessDeniedException ex) throws IOException {

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                403,
                "Forbidden",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        writeJson(response, 403, body);
    }

    private void writeJson(HttpServletResponse response,
                           int status,
                           ApiErrorResponse body) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), body);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {//todo checken
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));          // alle Origins
        config.setAllowedMethods(List.of("GET", "POST", "PUT",
                "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);                      // zum Testen einfacher

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /*
    @Bean//todo move or rename file
    public WebMvcConfigurer disableDefaultErrorPages() {
        return new WebMvcConfigurer() {
            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                configurer.setUseTrailingSlashMatch(true);
            }
        };
    }*/

    // Passwort-Encoder
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Auth Provider (Prüft Passwort + lädt Benutzer)
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(benutzerDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // Auth Manager (Benutzt unseren Provider)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
