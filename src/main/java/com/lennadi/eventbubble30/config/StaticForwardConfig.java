package com.lennadi.eventbubble30.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticForwardConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/reset-password")
                .setViewName("forward:/reset-password/index.html");

        registry.addViewController("/verify-email")
                .setViewName("forward:/verify-email/index.html");
    }
}

