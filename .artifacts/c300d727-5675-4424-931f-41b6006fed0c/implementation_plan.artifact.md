# Implementation Plan - UI Simplification & Log Fixes

This plan reverts the "Assist" and "Undo on Team Page" features while fixing the "Undo in Menu" and "Kickoff Log" issues.

## User Review Required

> [!IMPORTANT]
> - **Undo:** The 🔄 button will be removed from the team actions page but will remain (and be fixed) in the game settings menu.
> - **Log:** The kickoff team will now be logged correctly for every half, even if started from the settings menu.
> - **Assists:** The "Assist?" question after a goal will be removed to keep the interface simple.

## Proposed Changes

### [common] (Shared Logic)

#### [MODIFY] [GameEvent.kt](file:///C:/Users/Jan/Downloads/RefWatch/common/src/main/java/com/databelay/refwatch/common/GameEvent.kt)
- Remove `assistantNumber` from `GoalScoredEvent`.
- Revert `displayString` to exclude assist information.

### [wear] (Watch App)

#### [MODIFY] [WearGameViewModel.kt](file:///C:/Users/Jan/Downloads/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/WearGameViewModel.kt)
- Update `toggleTimer()`: Add logic to log a "Kick-off" event if the timer is started at 0:00 in a playable phase.
- Update `addGoal()`: Remove `assistantNumber` parameter.
- Ensure `undoLastEvent()` correctly removes the latest event and syncs.

#### [MODIFY] [LogGoalScreen.kt](file:///C:/Users/Jan/Downloads/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/LogGoalScreen.kt)
- Revert to a single-step goal logging process (remove "Assist?" step).

#### [MODIFY] [TeamActionsPage.kt](file:///C:/Users/Jan/Downloads/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/TeamActionsPage.kt)
- Remove the Undo (🔄) button from the header.

#### [MODIFY] [GameSettingsScreen.kt](file:///C:/Users/Jan/Downloads/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/GameSettingsScreen.kt)
- Ensure `onUndoLastEvent` is properly wired up.
- (Optional) Style the Undo button in the menu for better visibility.

## Verification Plan

### Manual Verification
1.  **Kickoff Log**: Start a game half from the menu (Play button). Verify "Anstoß..." appears in the log.
2.  **Undo in Menu**: Log a goal, go to settings, click Undo. Verify the score reverts.
3.  **Team Actions**: Swipe to the team page and verify the 🔄 button is gone.
4.  **Goal Screen**: Log a goal and verify it returns to the main screen immediately after picking the player.
