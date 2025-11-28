package com.lennadi.eventbubble30.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

public class ServerConfigSetup {
    @Bean
    CommandLineRunner initServerConfig(ServerConfigRepository rp) {
        return args ->{
            if(!rp.existsById(1L)){
                ServerConfigSingletonEntity cfg = new ServerConfigSingletonEntity();
                cfg.setGlobalTokensInvalidatedAt(Instant.EPOCH);
                rp.save(cfg);
            }
        };
    }
}
