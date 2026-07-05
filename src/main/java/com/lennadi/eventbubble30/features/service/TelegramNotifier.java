package com.lennadi.eventbubble30.features.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TelegramNotifier { // todo only behind profile

    private final RestClient restClient = RestClient.create();

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

    private volatile boolean shouldFilterRequests = true;
    private volatile boolean settingWasUpdatedThisSession = false;
    private volatile long lastUpdateId = 0L;

    @Async
    public void send(String text) {
        try {
            log.info("Sending Telegram notification ({} chars)", text != null ? text.length() : 0);

            restClient.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", text
                    ))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Telegram notification sent successfully");
        } catch (Exception ex) {
            log.warn("Failed to send Telegram notification: {}", ex.getMessage(), ex);
        }
    }

    public boolean getShouldFilterRequests() {
        return shouldFilterRequests;
    }

    public boolean getSettingWasUpdatedThisSession() {
        return settingWasUpdatedThisSession;
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void updateFilterRequests() {
        try {
            TelegramGetUpdatesResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.telegram.org")
                            .path("/bot{token}/getUpdates")
                            .queryParam("offset", lastUpdateId + 1)
                            .queryParam("limit", 20)
                            .queryParam("timeout", 0)
                            .build(botToken))
                    .retrieve()
                    .body(TelegramGetUpdatesResponse.class);

            if (response == null || response.result() == null || response.result().isEmpty()) {
                if (!settingWasUpdatedThisSession) {
                    settingWasUpdatedThisSession = true;
                    send("Request filtering is currently ON (default for this session).");
                }
                return;
            }

            boolean oldValue = shouldFilterRequests;

            for (TelegramUpdate update : response.result()) {
                if (update.updateId() != null && update.updateId() > lastUpdateId) {
                    lastUpdateId = update.updateId();
                }

                String text = extractText(update);
                if (text == null) {
                    continue;
                }

                String normalized = text.trim().toLowerCase();

                if (normalized.equals("/filter on")) {
                    shouldFilterRequests = true;
                } else if (normalized.equals("/filter off")) {
                    shouldFilterRequests = false;
                } else if (normalized.equals("/filter")) {
                    send("Request filtering is currently " + (shouldFilterRequests ? "ON" : "OFF") + ".");
                }
            }

            boolean changed = oldValue != shouldFilterRequests;
            boolean firstUpdateThisSession = !settingWasUpdatedThisSession;

            if (changed) {
                send("Request filtering was turned " + (shouldFilterRequests ? "ON" : "OFF") + ".");
            } else if (firstUpdateThisSession) {
                send("Request filtering is currently " + (shouldFilterRequests ? "ON" : "OFF") + ".");
            }

            if (!settingWasUpdatedThisSession) {
                settingWasUpdatedThisSession = true;
            }
        } catch (Exception ex) {
            log.warn("Failed to update request filtering setting from Telegram: {}", ex.getMessage(), ex);
        }
    }

    private String extractText(TelegramUpdate update) {
        if (update == null) {
            return null;
        }
        if (update.message() != null && update.message().text() != null) {
            return update.message().text();
        }
        if (update.editedMessage() != null && update.editedMessage().text() != null) {
            return update.editedMessage().text();
        }
        return null;
    }

    private record TelegramGetUpdatesResponse(
            boolean ok,
            List<TelegramUpdate> result
    ) {}

    private record TelegramUpdate(
            @JsonProperty("update_id") Long updateId,
            TelegramMessage message,
            @JsonProperty("edited_message") TelegramMessage editedMessage
    ) {}

    private record TelegramMessage(
            String text
    ) {}
}