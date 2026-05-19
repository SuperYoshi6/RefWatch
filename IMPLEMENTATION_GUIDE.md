# 🎯 Quick Integration Guide: Neue Features

## 1. Torart-Eingabe in MatchScreen integrieren

### Schritt 1: State hinzufügen (in MatchViewModel oder MatchScreen)
```kotlin
var showGoalDialog by remember { mutableStateOf(false) }
var selectedTeamForGoal by remember { mutableStateOf<Team?>(null) }
```

### Schritt 2: Team-Namen clickable machen
```kotlin
// In der UI wo Teamnamen angezeigt werden:
Text(
    text = game.homeTeamName,
    modifier = Modifier
        .clickable {
            selectedTeamForGoal = Team.HOME
            showGoalDialog = true
        }
        .padding(8.dp),
    style = MaterialTheme.typography.headlineSmall
)

// Analog für awayTeamName
```

### Schritt 3: Dialog anzeigen
```kotlin
// Nach dem UI-Code:
if (showGoalDialog && selectedTeamForGoal != null) {
    GoalInputDialog(
        teamName = when (selectedTeamForGoal) {
            Team.HOME -> game.homeTeamName
            Team.AWAY -> game.awayTeamName
            else -> "Team"
        },
        team = selectedTeamForGoal!!,
        onGoalTypeSelected = { goalType ->
            // Tore mit Torart aufzeichnen
            viewModel.recordGoal(
                team = selectedTeamForGoal!!,
                goalType = goalType,
                playerNumber = null  // Optional: Spielernummer später hinzufügen
            )
            showGoalDialog = false
            selectedTeamForGoal = null
        },
        onDismiss = {
            showGoalDialog = false
            selectedTeamForGoal = null
        }
    )
}
```

### Schritt 4: ViewModel-Funktion
```kotlin
// In MatchViewModel.kt
fun recordGoal(team: Team, goalType: GoalType, playerNumber: Int? = null) {
    val goalEvent = GoalScoredEvent(
        team = team,
        goalType = goalType,
        playerNumber = playerNumber,
        gameTimeMillis = currentDisplayTimeMillis.toDouble()
    )
    
    // Event zur Liste hinzufügen
    _gameEvents.value = _gameEvents.value + goalEvent
    
    // Score aktualisieren
    when (team) {
        Team.HOME -> _homeScore.value++
        Team.AWAY -> _awayScore.value++
    }
    
    // Optional: Vibration Feedback
    context.vibrate(HapticFeedbackConstants.CONFIRM)
}
```

---

## 2. Performance-Optimierungen sofort anwenden

### Quick Win 1: GameListScreen LazyColumn Keys
```kotlin
// IN: common/src/main/java/com/databelay/refwatch/screens/GameListScreen.kt
// Zeile ~200 (ungefähr):

LazyColumn(
    state = lazyListState,
    contentPadding = paddingValues,
    modifier = Modifier.fillMaxSize()
) {
    items(
        gamesToDisplay,
        key = { game -> game.id }  // ← ADD THIS LINE
    ) { game ->
        ExplanationArea(
            tag = "game_card_${game.id}",
            // ...
```

### Quick Win 2: Timer StateFlow
```kotlin
// In deinem ViewModel (z.B. MatchViewModel):

private val _displayTimeMillis = MutableStateFlow(0L)
val displayTimeMillis: StateFlow<Long> = _displayTimeMillis.asStateFlow()

fun startTimer() {
    timerJob = viewModelScope.launch {
        while (isTimerRunning) {
            delay(100)  // Update alle 100ms statt 17ms
            _displayTimeMillis.value += 100
        }
    }
}

// In UI:
val displayTime by viewModel.displayTimeMillis.collectAsStateWithLifecycle()
```

### Quick Win 3: Remember stabile Referenzen
```kotlin
// Im MatchScreen oder GameListScreen:

val displaySeconds = remember(displayTimeMillis) {
    (displayTimeMillis / 1000).toInt()
}

val displayText = remember(displaySeconds) {
    val minutes = displaySeconds / 60
    val seconds = displaySeconds % 60
    String.format("%02d:%02d", minutes, seconds)
}

Text(displayText)  // Recomposed nur bei echtem Change
```

---

## 3. Überarbeitete SettingsScreen nutzen

### Schritt 1: Navigation anpassen (in MainActivity oder NavGraph)
```kotlin
// Falls noch nicht gemacht: neue Navigation Route
sealed class NavRoute {
    object Settings : NavRoute()
    object SettingsNew : NavRoute()  // Optional: zweite Version testen
    // ...
}

// Im Composable NavHost:
composable(route = NavRoute.Settings::class.simpleName!!) {
    SettingsScreenNew(  // Nutze die neue Version
        onNavigateBack = { /* pop back stack */ },
        onDeleteAccountConfirmed = { /* logout */ },
        onDeleteAllCompletedGames = { /* delete */ }
    )
}
```

### Schritt 2: Callbacks im Fragment/Activity implementieren
```kotlin
// In deinem Activity/Fragment:
val callbacks = object : SettingsCallbacks {
    override fun onDeleteAccountConfirmed() {
        // Firebase Auth abmelden
        firebaseAuth.signOut()
        // Navigation zur LoginScreen
        navController.navigate(NavRoute.Login)
    }
    
    override fun onDeleteAllCompletedGames() {
        // Alle Games mit status COMPLETED löschen
        viewModel.deleteAllCompletedGames()
    }
}
```

---

## 4. Testen der neuen Features

### Unit Tests für GoalInputDialog
```kotlin
@RunWith(AndroidJUnit4::class)
class GoalInputDialogTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testGoalTypeSelection() {
        var selectedType: GoalType? = null
        
        composeTestRule.setContent {
            GoalInputDialog(
                teamName = "FC München",
                team = Team.HOME,
                onGoalTypeSelected = { selectedType = it },
                onDismiss = {}
            )
        }
        
        // Click auf Freistoß
        composeTestRule.onNodeWithText("Freistoß").performClick()
        composeTestRule.onNodeWithText("Bestätigen").performClick()
        
        assertEquals(GoalType.FREE_KICK, selectedType)
    }
}
```

### Performance Test
```kotlin
@get:Rule
val benchmarkRule = BenchmarkRule()

@Test
fun benchmarkGameListRendering() {
    benchmarkRule.measureRepeated {
        composeTestRule.setContent {
            GameListScreen(
                games = generateTestGames(1000),  // 1000 games
                // ...
            )
        }
    }
}
```

---

## 5. Deployment Checkliste

- [ ] GoalType Enum erweitert ✅
- [ ] GoalInputDialog.kt erstellt ✅
- [ ] SettingsScreenNew.kt erstellt ✅
- [ ] GoalInputDialog in MatchScreen integriert
- [ ] Timer auf StateFlow umgestellt
- [ ] LazyColumn Keys hinzugefügt
- [ ] Vibration Feedback bei Toren hinzugefügt
- [ ] Unit Tests geschrieben
- [ ] auf echtem Device testen (Handy + Watch)
- [ ] Performance mit Android Profiler messen
- [ ] User-Feedback sammeln

---

## 6. Debugging & Monitoring

### Logging
```kotlin
// Nutze Log.d() für Performance-kritische Teile
Log.d("MatchScreen", "Game state updated, recomposed at ${System.currentTimeMillis()}")
```

### Android Profiler Metriken zu prüfen:
1. **CPU** - sollte <30% im idle sein
2. **Memory** - sollte <150MB für die App sein
3. **GPU** - Frame drops sollten <2% sein
4. **Battery** - sollte <5% die Stunde verbrauchen

### Tipps zum Testen:
```bash
# Auf echtem Device über Logcat filtern:
adb logcat | grep "RefWatch"

# ANR (Application Not Responding) Logs:
adb logcat | grep "ANR"

# Battery Stats:
adb shell dumpsys batterystats --reset
```

---

## 7. Weitere Performance-Ideen

1. **Image Lazy Loading** für Team-Logos
2. **Paginierung** in Event-Log (wenn >100 Events)
3. **Background Sync** für Firestore Updates
4. **Bluetooth Optimization** zwischen Watch & Phone
5. **Wear OS Always-On Display** Optimierung

---

## 📞 Support

Falls Fragen entstehen:
- Prüfe PERFORMANCE_OPTIMIZATIONS.md für Details
- Nutze Android Studio Profiler für Debugging
- Teste auf realen Geräten (Wear OS ist anders als Handy!)
