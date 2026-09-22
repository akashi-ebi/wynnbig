/**
 * Wynn Lootrun Logger — webhook receiver.
 *
 * SETUP:
 * 1. Make a Google Sheets copy of Wynncraft_Lootrun_Profit_Tracker.xlsx
 *    (File > Import in Sheets, or just upload the xlsx and it'll convert).
 *    The "Run Log" tab must keep its header row (row 4) text exactly as-is
 *    — this script matches columns by header name, not by letter, so
 *    reordering columns is fine, renaming headers is not.
 * 2. In that Sheet: Extensions > Apps Script.
 * 3. Delete the placeholder code, paste this whole file in.
 * 4. Deploy > New deployment > type "Web app".
 *      - Execute as: Me
 *      - Who has access: Anyone (Apps Script requires this for a mod to
 *        POST to it without a Google login; the URL itself is your secret —
 *        don't share it)
 * 5. Copy the Web App URL it gives you (ends in /exec) into
 *    config/lootrun-logger.json as "webhookUrl" on the mod side.
 * 6. IMPORTANT: clear/delete the EXAMPLE row (row 7) in Run Log before
 *    your first real run — this script appends after the last row that has
 *    a Run # filled in, so a leftover example row just becomes Run #1.
 */

const SHEET_NAME = "Run Log";
const HEADER_ROW = 4;
const FIRST_DATA_ROW = 7;

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    const payload = JSON.parse(e.postData.contents);
    const sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(SHEET_NAME);
    if (!sheet) {
      return jsonResponse({ ok: false, error: `No sheet named "${SHEET_NAME}" found.` });
    }

    const headers = sheet.getRange(HEADER_ROW, 1, 1, sheet.getLastColumn()).getValues()[0];
    const colIndex = {};
    headers.forEach((h, i) => {
      if (h) colIndex[String(h).trim()] = i + 1; // 1-based column
    });

    const targetRow = findNextEmptyRow(sheet, colIndex["Run #"]);
    const runNumber = targetRow - FIRST_DATA_ROW + 1;

    const missions = payload.missions || [];
    const trials = payload.trials || [];

    setCell(sheet, targetRow, colIndex["Run #"], runNumber);
    setCell(sheet, targetRow, colIndex["Date"], new Date());
    setCell(sheet, targetRow, colIndex["Camp"], payload.campName || "");
    setCell(sheet, targetRow, colIndex["Run length (min)"],
        payload.timeElapsedSeconds ? Math.round((payload.timeElapsedSeconds / 60) * 10) / 10 : "");
    setCell(sheet, targetRow, colIndex["Challenges completed"], payload.challengesCompleted || "");
    setCell(sheet, targetRow, colIndex["Raw pulls (end screen)"], payload.rewardPulls || 0);
    setCell(sheet, targetRow, colIndex["Rerolls (end screen)"], payload.rewardRerolls || 0);
    setCell(sheet, targetRow, colIndex["Sacrifices (end screen)"], payload.rewardSacrifices || 0);
    setCell(sheet, targetRow, colIndex["Mission 1"], missions[0] || "");
    setCell(sheet, targetRow, colIndex["Mission 2"], missions[1] || "");
    setCell(sheet, targetRow, colIndex["Mission 3"], missions[2] || "");
    setCell(sheet, targetRow, colIndex["Mission 4"], missions[3] || "");
    setCell(sheet, targetRow, colIndex["Trial 1"], trials[0] || "");
    setCell(sheet, targetRow, colIndex["Trial 2"], trials[1] || "");

    if (payload.status === "failed") {
      setCell(sheet, targetRow, colIndex["Notes"], "Auto-logged: run failed/left early");
    }

    return jsonResponse({ ok: true, row: targetRow, runNumber: runNumber });
  } catch (err) {
    return jsonResponse({ ok: false, error: err.message });
  } finally {
    lock.releaseLock();
  }
}

function doGet(e) {
  return jsonResponse({ ok: true, message: "Wynn Lootrun Logger webhook is alive." });
}

function findNextEmptyRow(sheet, runNumberCol) {
  const lastRow = sheet.getLastRow();
  if (lastRow < FIRST_DATA_ROW) return FIRST_DATA_ROW;

  const values = sheet
      .getRange(FIRST_DATA_ROW, runNumberCol, lastRow - FIRST_DATA_ROW + 1, 1)
      .getValues();

  for (let i = 0; i < values.length; i++) {
    if (values[i][0] === "" || values[i][0] === null) {
      return FIRST_DATA_ROW + i;
    }
  }
  return lastRow + 1;
}

function setCell(sheet, row, col, value) {
  if (!col) return; // header not found — skip silently rather than crash
  sheet.getRange(row, col).setValue(value);
}

function jsonResponse(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
