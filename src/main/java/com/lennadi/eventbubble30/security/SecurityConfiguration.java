package com.lennadi.eventbubble30.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final BenutzerDetailsService benutzerDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                // CSRF für REST deaktivieren
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/user/create").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
                        .requestMatchers("/api/auth/logout").authenticated()
                        .anyRequest().authenticated()   // Rest geschützt
                )
                .formLogin(login -> login.disable()) // wir machen unser eigenes Login
                .httpBasic(basic -> basic.disable()) // Basic-Auth aus
                .logout(logout -> logout.disable())  // wir machen eigenes Logout
                .sessionManagement(sess -> sess
                        .maximumSessions(1)          // nur 1 Login pro User
                );

        return http.build();
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
