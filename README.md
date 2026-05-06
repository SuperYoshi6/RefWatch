# RefWatch – Wear OS Schiedsrichter-App

![RefWatch Icon](./ic_launcher-playstore.png)

RefWatch ist eine Wear-OS- und Android-App zur Unterstützung von Fußball-Schiedsrichtern direkt am Handgelenk.

## Deutsch

### Funktionen
- Match-Setup mit Teamnamen, Teamkürzeln, Farben, Kapitänsnummern, Halbzeitdauer, Pausendauer und max. Wechseln.
- Spieluhr mit Umschaltung zwischen Restzeit und gespielter Zeit.
- Nachspielzeit-Timer (grün), der separat gestartet/gestoppt werden kann.
- 5-Sekunden-Anstoß-Countdown mit Vibrationsfeedback.
- Ereignis-Erfassung: Tore, Karten, Wechsel, Spielprotokoll.
- Aktionsmenü per Long-Press im Hauptscreen (z. B. Protokoll, Halbzeit beenden, Timer-Aktionen).

### Projektstruktur
- `wear/` – Wear OS App (Hauptspielbetrieb auf der Uhr)
- `mobile/` – Android Begleit-App (komfortables Setup/Verwaltung)
- `common/` – Gemeinsame Modelle und Logik
- `functions/` – Firebase Functions
- `landingpage/` – einfache Download-Landingpage

### Build & Start
1. Repository klonen:
   ```bash
   git clone https://github.com/SuperYoshi6/RefWatch.git
   cd RefWatch
   ```
2. In Android Studio öffnen.
3. Build ausführen:
   ```bash
   ./gradlew :wear:assembleDebug :mobile:assembleDebug
   ```
4. Uhr/Emulator auswählen und Wear-App starten.

### Firebase (optional)
```bash
firebase login
firebase init functions
cd functions
firebase deploy --only functions
```

### Hinweise
- Für beste Eingabe-Erfahrung Teamdaten in der Mobile-App pflegen und auf der Uhr nutzen.
- Das Aktionsmenü ist für den Spielbetrieb auf Long-Press ausgelegt.

---

# RefWatch – Wear OS Referee App

![RefWatch Icon](./ic_launcher-playstore.png)

RefWatch is a Wear OS + Android app that helps football referees manage matches directly from the wrist.

## English

### Features
- Match setup with team names, abbreviations, colors, captain numbers, half-time duration, break duration, and max substitutions.
- Main timer with toggle between remaining time and played time.
- Separate stoppage-time timer (green), start/stop independently.
- 5-second kickoff countdown with vibration feedback.
- Event logging: goals, cards, substitutions, and full match log.
- Long-press action menu on the main screen (e.g., log view, end half, timer actions).

### Project Structure
- `wear/` – Wear OS app (main in-match experience)
- `mobile/` – Android companion app (easier setup/management)
- `common/` – shared models and logic
- `functions/` – Firebase Functions
- `landingpage/` – simple download landing page

### Build & Run
1. Clone repository:
   ```bash
   git clone https://github.com/SuperYoshi6/RefWatch.git
   cd RefWatch
   ```
2. Open in Android Studio.
3. Build:
   ```bash
   ./gradlew :wear:assembleDebug :mobile:assembleDebug
   ```
4. Select watch/emulator and run the Wear app.

### Firebase (optional)
```bash
firebase login
firebase init functions
cd functions
firebase deploy --only functions
```

### Notes
- For faster setup, edit team data in the mobile app and use it on the watch.
- The action menu is intentionally optimized for long-press during live matches.
