package com.lennadi.eventbubble30.config;

import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class CorsConfig implements WebMvcConfigurer {//todo checken
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")   // alle Origins erlauben
                .allowedMethods("*")          // GET, POST, PUT, DELETE, OPTIONS, ...
                .allowedHeaders("*")
                .allowCredentials(false);     // zum Testen erstmal false (mit "*" einfacher)
    }
}
