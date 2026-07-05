package com.lennadi.eventbubble30.features.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service @Slf4j
public class TelegramNotifier {//todo only behind profile

    private final RestClient restClient = RestClient.create();

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.chat-id}")
    private String chatId;

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
}