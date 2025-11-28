package com.lennadi.eventbubble30.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerConfig {

    public static String DOMAIN;

    @Value("${server.domain:localhost:8080}")
    public void setDomain(String domain) {
        DOMAIN = domain;
    }
}
