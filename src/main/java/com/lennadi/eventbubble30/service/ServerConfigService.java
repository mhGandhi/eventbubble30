package com.lennadi.eventbubble30.service;

import com.lennadi.eventbubble30.entities.ServerConfig;
import com.lennadi.eventbubble30.repository.ServerConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ServerConfigService {
    private final ServerConfigRepository repo;

    private static final Long SINGLETON_ID = 1L;

    public Instant getGlobalTokenRevokationTime()
    {
        return repo.findById(SINGLETON_ID)
                .map(ServerConfig::getGlobalTokensInvalidatedAt)
                .orElse(Instant.EPOCH);
    }

    public void invalidateAllTokensNow(){
        ServerConfig cfg = repo.findById(SINGLETON_ID)
                .orElseGet(()->{
                    ServerConfig c = new ServerConfig();
                    c.setId(SINGLETON_ID);
                    return c;
                });
        cfg.setGlobalTokensInvalidatedAt(Instant.now());
        repo.save(cfg);
    }
}
