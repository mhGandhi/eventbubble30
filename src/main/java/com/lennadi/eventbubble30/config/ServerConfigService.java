package com.lennadi.eventbubble30.config;

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
                .map(ServerConfigSingletonEntity::getGlobalTokensInvalidatedAt)
                .orElse(Instant.EPOCH);
    }

    public void invalidateAllTokensNow(){
        ServerConfigSingletonEntity cfg = repo.findById(SINGLETON_ID)
                .orElseGet(()->{
                    ServerConfigSingletonEntity c = new ServerConfigSingletonEntity();
                    c.setId(SINGLETON_ID);
                    return c;
                });
        cfg.setGlobalTokensInvalidatedAt(Instant.now());
        repo.save(cfg);
    }
}
