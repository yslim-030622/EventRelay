package com.eventrelay.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DiscordAlertService {

    @Value("${app.discord-webhook-url:}")
    private String discordWebhookUrl;

    private final WebClient webClient = WebClient.create();

    public void sendAlert(String title, String description, int color) {
        if (discordWebhookUrl == null || discordWebhookUrl.isEmpty()) {
            log.debug("Discord webhook URL not configured, skipping alert");
            return;
        }

        Map<String, Object> embed = Map.of(
            "title", title,
            "description", description,
            "color", color
        );

        try {
            webClient.post()
                .uri(discordWebhookUrl)
                .bodyValue(Map.of("embeds", List.of(embed)))
                .retrieve()
                .toBodilessEntity()
                .block();
            log.info("Discord alert sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send Discord alert: {}", e.getMessage());
        }
    }
}
