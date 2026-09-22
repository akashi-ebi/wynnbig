package com.aaa.lootrunlogger;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LootrunWebhookClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Fires the POST off the main thread so a slow/broken webhook never freezes the game. */
    public static void send(LootrunRunRecord record) {
        String webhookUrl = LootrunLoggerConfig.get().webhookUrl;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            LootrunLoggerMod.LOGGER.warn(
                    "[LootrunLogger] No webhookUrl configured in config/lootrun-logger.json — run not sent.");
            return;
        }

        String json = GSON.toJson(record);

        Thread.ofVirtual().start(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    LootrunLoggerMod.LOGGER.info("[LootrunLogger] Run logged: " + json);
                } else {
                    LootrunLoggerMod.LOGGER.warn("[LootrunLogger] Webhook returned "
                            + response.statusCode() + ": " + response.body());
                }
            } catch (Exception e) {
                LootrunLoggerMod.LOGGER.error("[LootrunLogger] Failed to send run to webhook", e);
            }
        });
    }
}
