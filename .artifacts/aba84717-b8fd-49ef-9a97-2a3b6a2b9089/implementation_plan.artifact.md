# Implementation Plan - Multi-Subs, Shortcut Guards & Strict Transitions

This plan addresses user feedback regarding bulk substitution entry, UI safety during breaks, and more controlled match phase transitions.

## User Review Required

> [!IMPORTANT]
> **Bulk Substitutions**: I will allow entering multiple player numbers separated by `,` or `+`. If you enter `5, 7` as outgoing and `10, 12` as incoming, the app will log two separate substitution events (#5 ➡️ #10 and #7 ➡️ #12). If the counts don't match, only the matching pairs will be processed.
>
> **Shortcut Locking**: Gestures (long-press/double-tap) for logging goals and substitutions will be disabled during half-time breaks to prevent accidental entries.

## Proposed Changes

### [Common] Logic

#### [MODIFY] [DataModels.kt](file:///C:/Users/Jan/Downloads/Games%20und%20Apps/RefWatch/common/src/main/java/com/databelay/refwatch/common/DataModels.kt)
- No changes needed here for this phase.

### [Wear] Data & State

#### [MODIFY] [WearGameViewModel.kt](file:///C:/Users/Jan/Downloads/Games%20und%20Apps/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/WearGameViewModel.kt)
- **Multi-Sub Support**: Add `logMultipleSubstitutions(team: Team, outgoingStr: String, incomingStr: String)` which parses the strings and calls the existing `logSubstitution` for each pair.
- **Phase Logic**: Refine `proceedToNextPhaseManager` to skip Extra Time/Penalties if they are disabled in game settings, even if the score is tied.

### [Wear] Presentation & UI

#### [MODIFY] [LogSubstitutionScreen.kt](file:///C:/Users/Jan/Downloads/Games%20und%20Apps/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/LogSubstitutionScreen.kt)
- Update `OutlinedTextField` to allow `,` and `+` characters.
- Change `KeyboardType` to `Text`.
- Call the new multi-sub function in the ViewModel.

#### [MODIFY] [QuickSubstitutionDialog.kt](file:///C:/Users/Jan/Downloads/Games%20und%20Apps/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/QuickSubstitutionDialog.kt)
- Same updates as `LogSubstitutionScreen` for consistency.

#### [MODIFY] [MainGameDisplayScreen.kt](file:///C:/Users/Jan/Downloads/Games%20und%20Apps/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/MainGameDisplayScreen.kt)
- Wrap goal and substitution gestures in `if (!currentPhase.isBreak())` check.

#### [MODIFY] [TeamActionsPage.kt](file:///C:/Users/Jan/Downloads/Games%20und%20Apps/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/TeamActionsPage.kt)
- Disable the "+1" goal button and substitution button if the match is in a break phase.
- Disable gestures on the team name header during breaks.

#### [MODIFY] [GameSettingsScreen.kt](file:///C:/Users/Jan/Downloads/Games%20und%20Apps/RefWatch/wear/src/main/java/com/databelay/refwatch/wear/presentation/screens/GameSettingsScreen.kt)
- Adjust "End Phase" button logic to better reflect the next state (e.g. "End Match" if no ET/Pens are enabled).

## Verification Plan

### Automated Tests
- Build both `:mobile:assembleDebug` and `:wear:assembleDebug`.

### Manual Verification
1.  **Multi-Subs**: Open substitution screen. Type `1, 2` for OUT and `3, 4` for IN. Verify game log shows two substitutions.
2.  **Break Guard**: During half-time, try double-tapping a team color or long-pressing. Verify no dialog opens.
3.  **Phase Skip**: Set a game to `hasExtraTime = false`. Draw the match. End the 2nd half. Verify it goes to `GAME_ENDED` directly.
