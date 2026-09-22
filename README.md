# Wynn Lootrun Logger

A small Fabric client mod that auto-captures Wynncraft lootrun run data (pulls,
rerolls, sacrifices, time, missions, trials) and POSTs it to a webhook so it
lands straight in your Lootrun Profit Tracker sheet — no manual entry.

It works by depending on [Wynntils](https://github.com/Wynntils/Wynntils)
(LGPLv3, open source) as a library and listening to its public
`LootrunFinishedEvent.Completed` event plus a couple of public getters on
`Models.Lootrun`. It does not read, copy, or depend on any other mod.

## What it captures automatically

| Field | Source |
|---|---|
| Challenges completed | `LootrunFinishedEvent.Completed#getChallengesCompleted()` |
| Run time (seconds) | `#getTimeElapsed()` |
| Raw pulls | `#getRewardPulls()` |
| Rerolls | `#getRewardRerolls()` |
| Sacrifices | `#getRewardSacrifices()` |
| XP gained / mobs killed / chests opened | bonus fields, same event |
| Missions selected (up to 4, in order) | `Models.Lootrun.getMissionStatus(0..3, false)` |
| Trials selected (up to 2, in order) | `Models.Lootrun.getTrial(0..1)` |

**Not captured** (would need reverse-engineering the mission/trial *choice*
GUI, which Wynntils itself doesn't track): which missions/trials were
*offered but not picked*. Your spreadsheet only needs what was picked, so
this isn't a real gap for the profit tracker — just flagging it so it's not
a surprise.

## One-time setup

I can't run Minecraft or Gradle in the sandbox I built this in, so I can't
test-compile this myself. Do this locally:

### 1. Generate the base project

Go to **https://fabricmc.net/develop/template/**, and fill in:
- Minecraft version: **1.21.11**
- Loader version: latest
- Mod name: `Wynn Lootrun Logger`
- Mod ID: `lootrun-logger`
- Package name: `com.aaa.lootrunlogger` (or your own — just update the
  `package` lines in the Java files below to match)
- Include Fabric API: **yes**
- Include example mod code: **no** (or delete it after)
- Include Fabric Example Mod's Java toolchain (Java 21): **yes**

Download and unzip it.

### 2. Drop these files in

Copy everything from this repo's `src/` folder into the generated project's
`src/` folder, overwriting `fabric.mod.json`. Keep the generated
`build.gradle`, `settings.gradle`, `gradle.properties`, and `gradlew*` files
— then apply the two small edits below to `build.gradle` and
`gradle.properties`.

### 3. Add Wynntils as a dependency

In `gradle.properties`, add:

```properties
wynntils_version=REPLACE_ME
```

Get the real value from Wynntils' Modrinth page:
**https://modrinth.com/mod/wynntils/versions** → filter to Minecraft
`1.21.11`, loader `Fabric` → open the newest matching version → copy the
**Version ID** shown under "Developer information" (a short string like
`aE2Xhw2B`) → that's your `wynntils_version`.

In `build.gradle`, add to `repositories`:

```groovy
repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = "https://api.modrinth.com/maven"
            }
        }
        filter { includeGroup "maven.modrinth" }
    }
}
```

and to `dependencies`:

```groovy
dependencies {
    modImplementation "maven.modrinth:dU5Gb9Ab:${project.wynntils_version}"
}
```

(`dU5Gb9Ab` is Wynntils' fixed Modrinth project ID — that part never
changes, only the version ID does.)

### 4. Build

```
./gradlew build
```

The jar comes out in `build/libs/`. Drop it, plus the matching Wynntils
jar, into your `mods` folder (Fabric API too, if not already there).

### 5. Configure the webhook

Launch the game once with the mod installed (it'll crash-safe if Wynntils
isn't found — check your log if nothing happens). This creates
`config/lootrun-logger.json`. Edit it:

```json
{
  "webhookUrl": "https://script.google.com/macros/s/XXXXX/exec",
  "campName": "Molten Heights"
}
```

`webhookUrl` is the Google Apps Script Web App URL — see
`google-apps-script/Code.gs` in this repo for the receiving end and
deployment steps. `campName` is whatever you want written into the Camp
column for runs logged in this session (leave it blank to fill in
manually later).

### 6. Run a lootrun and check the sheet

Every completed (or failed) run should now append as a new row.

## Repo layout

```
src/main/java/com/aaa/lootrunlogger/
  LootrunLoggerMod.java      entrypoint, subscribes to Wynntils events
  LootrunRunRecord.java      the JSON payload shape
  LootrunLoggerConfig.java   loads/saves config/lootrun-logger.json
  LootrunWebhookClient.java  fires the async HTTP POST
src/main/resources/
  fabric.mod.json
google-apps-script/
  Code.gs                    paste into a Google Sheets Apps Script project
```

## Status

Proof of concept. The event-driven fields (pulls/rerolls/sacrifices/time/
missions/trials) should be solid since they're Wynntils' own tracked data.
Untested end-to-end since I can't run a game client — expect to iterate on
this with me once you've got it building.
