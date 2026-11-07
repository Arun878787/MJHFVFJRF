# Baccarat AI • Speed Analyzer (Android prototype)

Target device: HONOR 400 Lite / Android 16. Package: `com.baccarat.ai`.

## Current prototype features
- User-approved Android MediaProjection screen capture.
- On-device Google ML Kit Latin text OCR; frames are not uploaded.
- Optional floating overlay showing recognized text.
- Best-effort extraction of visible P/B/T totals if the website exposes a legible text tally.

## Important limitations
This version is an OCR prototype. It does **not** reliably recognize the colored roadmap circles, card values, or every completed hand. It does not predict the next result. Baccarat outcomes are random; historical frequencies are descriptive only and are not betting advice.

## Build with GitHub Actions
1. Upload the **contents** of this folder to the repository root, preserving `app/` and `.github/` folders.
2. Open the **Actions** tab.
3. Select **Build Android APK** (not the separate `CI` workflow).
4. Select **Run workflow** on `main`.
5. Open the completed run and download the `BaccaratAI-debug-APK` artifact.
6. Extract the artifact ZIP and install `app-debug.apk` on the phone. Android may ask to allow installation from the browser/files app.

## First launch
1. Open the app.
2. Tap Start automatic screen reading.
3. Allow display over other apps when Android opens Settings, then return to the app.
4. Tap Start again and approve the Android screen-capture prompt.
5. A floating OCR panel appears. Stop it from the app with Stop analyzer.

Screen capture is started only after explicit Android consent. Do not use it to bypass website rules or capture private information.
