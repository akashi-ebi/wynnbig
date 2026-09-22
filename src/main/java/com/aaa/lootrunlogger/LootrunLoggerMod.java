package com.aaa.lootrunlogger;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.models.lootrun.event.LootrunFinishedEvent;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LootrunLoggerMod implements ClientModInitializer {
    public static final String MOD_ID = "lootrun-logger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LootrunLoggerConfig.load();

        // Wynntils exposes a plain, public event bus for exactly this kind of addon —
        // see WynntilsMod#registerListener in the Wynntils source.
        WynntilsMod.registerListener((LootrunFinishedEvent.Completed event) -> onCompleted(event));
        WynntilsMod.registerListener((LootrunFinishedEvent.Failed event) -> onFailed(event));

        LOGGER.info("[LootrunLogger] Initialized, listening for Wynntils lootrun events.");
    }

    private void onCompleted(LootrunFinishedEvent.Completed event) {
        LootrunRunRecord record = new LootrunRunRecord(
                Instant.now().toString(),
                "completed",
                LootrunLoggerConfig.get().campName,
                event.getChallengesCompleted(),
                event.getTimeElapsed(),
                event.getRewardPulls(),
                event.getRewardRerolls(),
                event.getRewardSacrifices(),
                event.getExperienceGained(),
                event.getMobsKilled(),
                event.getChestsOpened(),
                collectMissions(),
                collectTrials());

        LootrunWebhookClient.send(record);
    }

    private void onFailed(LootrunFinishedEvent.Failed event) {
        LootrunRunRecord record = new LootrunRunRecord(
                Instant.now().toString(),
                "failed",
                LootrunLoggerConfig.get().campName,
                event.getChallengesCompleted(),
                event.getTimeElapsed(),
                0,
                0,
                0,
                0,
                0,
                0,
                collectMissions(),
                collectTrials());

        LootrunWebhookClient.send(record);
    }

    /** Missions actually selected this run, in order (up to 4). Not the offered-but-unpicked options. */
    private List<String> collectMissions() {
        List<String> missions = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String mission = Models.Lootrun.getMissionStatus(i, false);
            if (mission == null || mission.isBlank() || mission.equalsIgnoreCase("Unknown")) break;
            missions.add(mission);
        }
        return missions;
    }

    /** Trials actually selected this run, in order (up to 2). */
    private List<String> collectTrials() {
        List<String> trials = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String trial = Models.Lootrun.getTrial(i);
            if (trial == null || trial.isBlank() || trial.equalsIgnoreCase("Unknown")) break;
            trials.add(trial);
        }
        return trials;
    }
}
