# Baccarat AI • Speed (Android starter)

Target device: HONOR 400 Lite / Android 16. Initial screen layout: supplied Stake Speed Baccarat A screenshot.

## Included in this starter
- Native Android Kotlin app shell.
- User-approved MediaProjection screen capture.
- Foreground screen-capture service notification.
- ImageReader frame capture pipeline.
- On-device ML Kit text recognition.
- Floating overlay displaying recognized screen text.

## Build APK with GitHub Actions
1. Upload the **contents** of this project to the root of your GitHub repository, keeping folders intact.
2. Ensure `.github/workflows/build-apk.yml` is present (not `blank.yml`).
3. Open the repository's Actions tab.
4. Select **Build Android APK**, then **Run workflow**.
5. When the run succeeds, open it and download the `baccarat-ai-debug-apk` artifact.

## Important limitations
This is an early OCR prototype, not a finished Baccarat analyzer. It reads visible text and displays OCR output. It does not yet reliably recognize the coloured roadmap circles, distinguish each completed hand, maintain a validated shoe history, or calculate predictions. The OCR needs calibration against the actual phone screen and browser scaling.

Screen capture requires explicit Android consent and a visible notification. Secure-screen protections are not bypassed. The app does not place bets or guarantee Baccarat outcomes.
