package com.lennadi.eventbubble30;

import com.lennadi.eventbubble30.entities.ServerConfig;
import com.lennadi.eventbubble30.repository.ServerConfigRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

public class ServerConfigSetup {
    @Bean
    CommandLineRunner initServerConfig(ServerConfigRepository rp) {
        return args ->{
            if(!rp.existsById(1L)){
                ServerConfig cfg = new ServerConfig();
                cfg.setGlobalTokensInvalidatedAt(Instant.EPOCH);
                rp.save(cfg);
            }
        };
    }
}
