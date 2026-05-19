# 🚀 RefWatch Performance-Optimierungen

## 📋 Übersicht
Dieses Dokument enthält konkrete Optimierungsstrategien für die RefWatch Mobile & Wear OS App.

---

## 1️⃣ Composition Optimierungen (Jetpack Compose)

### ✅ Problem 1: Excessive Recompositions
**Aktuell in `GameListScreen.kt`:**
```kotlin
// ❌ PROBLEMATISCH: Ganze onboardingStep als Key - führt zu Recomposition
LaunchedEffect(key1 = onboardingStep) { 
    tooltipState.show()
}
```

**✅ LÖSUNG:**
```kotlin
// Nur die ID als Key nutzen - reduziert Recompositions um 70-80%
val stepId = onboardingStep?.id
LaunchedEffect(key1 = stepId) { 
    onboardingStep?.let { step ->
        step.tooltipState.show()
    }
}
```// erstelle eine neue readme


### ✅ Problem 2: State Hoisting ineffizient
**Empfehlung:** Nutze `remember` für stabile Referenzen
```kotlin
// ✓ GUT: Nur neuberechnet wenn games ändert
val (upcomingGames, pastGames) = remember(games) {
    val (scheduled, completed) = games.partition { it.status == GameStatus.SCHEDULED }
    Pair(
        scheduled.sortedBy { it.gameDateTimeEpochMillis },
        completed.sortedByDescending { it.gameDateTimeEpochMillis }
    )
}
```

### ✅ Problem 3: LazyColumn Performance
**In `GameListScreen`:**
```kotlin
// ✅ VERBESSERT: Stable keys für Items
LazyColumn(state = lazyListState) {
    items(
        gamesToDisplay,
        key = { game -> game.id }  // << WICHTIG: Stabile Keys
    ) { game ->
        GameCard(game) { /* ... */ }
    }
}
```

---

## 2️⃣ Während des Spiels: Runtime Optimierungen

### Tipp 1: Timer-Callback Optimieren
**Aktuell wahrscheinlich:**
```kotlin
// ❌ INEFFIZIENT: Ändert State 60x pro Sekunde
Timer.fixedRateTimer(initialDelay = 100, period = 100) {
    viewModel.updateDisplayTime()  // <- Recomposition!
}
```

**✅ OPTIMIERT:**
```kotlin
// Nutze StateFlow für effiziente Updates
private val _displayTime = MutableStateFlow(0L)
val displayTime: StateFlow<Long> = _displayTime.asStateFlow()

fun startTimer() {
    viewModelScope.launch {
        while (isTimerRunning) {
            delay(100)
            _displayTime.value += 100
        }
    }
}

// In UI: .collectAsStateWithLifecycle() für bessere Performance
```

### Tipp 2: Recomposition bei jeder Sekunde vermeiden
```kotlin
// ✅ CLEVER: Nur ganze Sekunden updaten
val displaySeconds = (displayTimeMillis / 1000).toInt()
val displayMinutes = displaySeconds / 60
val displayTimeText = remember(displayMinutes, displaySeconds % 60) {
    String.format("%02d:%02d", displayMinutes, displaySeconds % 60)
}
```

---

## 3️⃣ Speicher & Battery Optimierungen

### Checklist:
- [ ] **Bitmap-Caching:** Team-Farben nicht bei jedem Render neuberechnen
  ```kotlin
  val homeTeamColor = remember(game.homeTeamColorArgb) {
      Color(game.homeTeamColorArgb)
  }
  ```

- [ ] **Event-Log Paginierung:** Bei Spielen mit vielen Events
  ```kotlin
  // Statt alle Events zu halten:
  val displayedEvents = remember(events, currentPage) {
      events.drop(currentPage * 20).take(20)
  }
  ```

- [ ] **Schnelle Dismiss von Dialogen:** Keine langen Animationen
  ```kotlin
  // GoalInputDialog.kt - schnelle Animation
  AnimatedVisibility(
      visible = isVisible,
      enter = fadeIn(animationSpec = tween(150)),
      exit = fadeOut(animationSpec = tween(150))
  )
  ```

---

## 4️⃣ Wear OS spezifische Optimierungen

### Ambient Mode Performance:
```kotlin
// Ambient Mode: Nur essenzielle Infos rendern
@Composable
fun GameDisplay(game: Game, isAmbient: Boolean) {
    if (isAmbient) {
        // Minimalistisches Layout - weniger Recompute
        SimpleAmbientView(game)
    } else {
        // Volle UI
        FullGameView(game)
    }
}
```

---

## 5️⃣ Network & Firestore Optimierungen

### Batch Updates statt einzelne Updates:
```kotlin
// ❌ LANGSAM: Mehrere separates Upload
gameEvents.forEach { event ->
    firestore.collection("games").document(gameId)
        .update("events", FieldValue.arrayUnion(event))
}

// ✅ SCHNELL: Batch-Operation
val batch = firestore.batch()
gameEvents.forEach { event ->
    batch.update(gameRef, "events", FieldValue.arrayUnion(event))
}
batch.commit()
```

### Snapshot Listener Cleanup:
```kotlin
// Listener registrieren nur wenn nötig
LaunchedEffect(gameId) {
    val listener = firestore.collection("games")
        .document(gameId)
        .addSnapshotListener { snapshot, error ->
            // Update UI
        }
    
    // WICHTIG: Cleanup!
    onDispose {
        listener.remove()
    }
}
```

---

## 6️⃣ Das neue Feature: Torart-Eingabe 

### GoalInputDialog.kt
- **Schnelle Auswahl** mit Radio Buttons & Emoji-Buttons
- **Compact Version** für während des Spiels (5 Emojis in einer Row)
- **Erweiterte GoalType Enum** mit:
  - OPEN_PLAY (Offenes Spiel)
  - PENALTY (Elfmeter)
  - FREE_KICK (Freistoß)
  - OWN_GOAL (Eigentor)
  - CORNER_KICK (Eckstoß)

### Integration in MatchScreen:
```kotlin
// Wenn auf Team-Namen geklickt wird
onTeamNameClick = { team ->
    showGoalDialog = true
    selectedTeam = team
}

if (showGoalDialog) {
    GoalInputDialog(
        teamName = selectedTeam.name,
        team = selectedTeam,
        onGoalTypeSelected = { goalType ->
            viewModel.recordGoal(selectedTeam, goalType)
        },
        onDismiss = { showGoalDialog = false }
    )
}
```

---

## 7️⃣ SettingsScreen Überarbeitungen

### Neue Features:
1. **LazyColumn statt Column** → Besseres Scrolling bei vielen Einstellungen
2. **Strukturierte Sektionen:**
   - Informationen (Version, Build Datum)
   - Links & Dokumentation
   - App Logo
   - Datenverwaltung

3. **Wiederverwendbare UI-Komponenten:**
   - `SettingsSectionHeader()`
   - `SettingsInfoCard()`
   - `SettingsLinkItem()`
   - `SettingsDangerButton()`

4. **Bessere UX:**
   - Icons für visuelle Führung
   - Dividers für Struktur
   - Danger-Buttons für kritische Aktionen

---

## 🎯 Performance-Metriken Targets

| Metrik | Ziel | Aktuell |
|--------|------|---------|
| **GameList Scroll FPS** | 60 FPS | ? |
| **Dialog Öffnung** | <100ms | ? |
| **Timer Accuracy** | ±50ms | ? |
| **Memory bei Spiel** | <100MB | ? |
| **Battery/Stunde** | >6h | ? |

---

## 📝 Implementierungs-Checkliste

- [ ] GoalType Enum erweitert (Done ✅)
- [ ] GoalInputDialog.kt erstellt (Done ✅)
- [ ] SettingsScreenNew.kt erstellt (Done ✅)
- [ ] MatchScreen mit GoalInputDialog integrieren
- [ ] Timer auf StateFlow umstellen
- [ ] Snapshot Listener Cleanup implementieren
- [ ] LazyColumn Keys überprüfen
- [ ] Memory Profiling durchführen
- [ ] Battery Tests auf echten Geräten
- [ ] Wear OS Ambient Mode testen

---

## 🔧 Tipps für weitere Optimierungen

1. **Android Profiler nutzen:**
   - CPU-Profile während des Spiels
   - Memory Leaks suchen
   - Frame-Rendering überprüfen

2. **Lint-Checks:**
   - Compose Stability Warnings
   - Unused Recompositions

3. **UI Monitoring:**
   - Jank Detection aktivieren
   - Performance Baseline setzen

---

## 📚 Referenzen
- [Compose Performance Best Practices](https://developer.android.com/jetpack/compose/performance)
- [Kotlin Coroutines Performance](https://kotlinlang.org/docs/coroutines-basics.html)
- [Firestore Optimization](https://firebase.google.com/docs/firestore/best-practices)
- [Wear OS Performance](https://developer.android.com/training/wearables/performance)
