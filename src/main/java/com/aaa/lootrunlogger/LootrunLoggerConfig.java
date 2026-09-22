package com.aaa.lootrunlogger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LootrunLoggerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("lootrun-logger.json");

    public String webhookUrl = "";
    public String campName = "";

    private static LootrunLoggerConfig instance;

    public static LootrunLoggerConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                instance = GSON.fromJson(json, LootrunLoggerConfig.class);
                if (instance == null) instance = new LootrunLoggerConfig();
            } else {
                instance = new LootrunLoggerConfig();
                save();
            }
        } catch (IOException e) {
            LootrunLoggerMod.LOGGER.error("Failed to load lootrun-logger.json, using defaults", e);
            instance = new LootrunLoggerConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(instance));
        } catch (IOException e) {
            LootrunLoggerMod.LOGGER.error("Failed to save lootrun-logger.json", e);
        }
    }
}
