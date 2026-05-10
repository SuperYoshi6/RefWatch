# RefWatch – Wear OS Schiedsrichter-App

![RefWatch Icon](./ic_launcher-playstore.png)

**100% Kostenlos & Open Source.** RefWatch ist eine professionelle Wear-OS- und Android-App, die Fußball-Schiedsrichter direkt am Handgelenk unterstützt. Optimiert für Performance und intuitive Bedienung unter Belastung.

## Deutsch

### Highlights
- **Lünetten-Steuerung (Rotary):** Nutze die Lünette oder Krone deiner Uhr (z.B. Galaxy Watch, Pixel Watch), um blitzschnell zwischen Spielanzeige und Einstellungen zu wechseln.
- **Ambient Mode (Always-On):** Stromsparende Anzeige für den Ruhemodus – die Spielzeit bleibt immer im Blick, ohne den Arm heben zu müssen.
- **Optimierte Performance:** Flüssige Bedienung auch auf älteren Wear-OS-Geräten durch effiziente Compose-Architektur.

### Funktionen
- Match-Setup: Teamnamen, Kürzel, Farben, Kapitäne, Halbzeitdauer, max. Wechsel.
- Spieluhr: Umschaltung Restzeit / gespielte Zeit.
- Nachspielzeit: Separater grüner Timer für Unterbrechungen.
- Anstoß-Countdown: 5-Sekunden-Timer mit Vibrationsfeedback.
- Ereignis-Log: Tore, Karten, Wechsel mit Zeitstempel.
- Aktionsmenü: Per Long-Press im Hauptscreen oder über die Settings-Page.

### Projektstruktur
- `wear/` – Wear OS App (Spielbetrieb)
- `mobile/` – Android App (Verwaltung & Setup)
- `common/` – Geteilte Datenmodelle & Logik
- `landingpage/` – Web-Landingpage

---

## English

**100% Free & Open Source.** RefWatch is a professional Wear OS + Android app that helps football referees manage matches directly from the wrist.

### Highlights
- **Rotary Input Support:** Use your watch's bezel or crown (e.g., Galaxy Watch, Pixel Watch) to quickly switch between match display and settings.
- **Ambient Mode (Always-On):** Power-efficient ambient view keeps the match time visible at all times.
- **Smooth Performance:** Optimized for lag-free operation even on older Wear OS hardware.

### Core Features
- Match Setup: Team names, abbreviations, colors, captains, half durations, max substitutions.
- Game Clock: Toggle between remaining and elapsed time.
- Stoppage Time: Separate green timer for injuries and delays.
- Kickoff Countdown: 5-second countdown with haptic feedback.
- Event Log: Goals, cards, and substitutions with timestamps.

---

### Build & Install
1. Clone: `git clone https://github.com/SuperYoshi6/RefWatch.git`
2. Build: `./gradlew assembleDebug`
3. Install: Use **Wear Installer 2** for the Watch APK.
