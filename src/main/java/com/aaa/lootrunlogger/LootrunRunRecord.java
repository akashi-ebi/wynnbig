package com.aaa.lootrunlogger;

import java.util.List;

/**
 * The JSON shape POSTed to the webhook. Field names are deliberately plain
 * and stable since the Google Apps Script on the receiving end matches
 * Run Log column headers by name (see google-apps-script/Code.gs).
 */
public record LootrunRunRecord(
        String timestamp,
        String status, // "completed" or "failed"
        String campName,
        int challengesCompleted,
        int timeElapsedSeconds,
        int rewardPulls,
        int rewardRerolls,
        int rewardSacrifices,
        int experienceGained,
        int mobsKilled,
        int chestsOpened,
        List<String> missions,
        List<String> trials) {}
