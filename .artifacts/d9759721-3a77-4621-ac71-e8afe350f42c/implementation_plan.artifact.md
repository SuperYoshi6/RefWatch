# Implementation Plan - Bug Fixes and Version Bump

This plan addresses a string mismatch in the "Delete All Games" confirmation dialog, adds missing English translations, and increments the app version to `1.6.2`.

## User Review Required

> [!IMPORTANT]
> The confirmation button for deleting all past games was incorrectly using the "Delete Account" string. I will switch it to a specific "Delete All Games" string.

## Proposed Changes

### Mobile App Logic

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/java/com/databelay/refwatch/screens/SettingsScreen.kt)
- Change the confirmation button text in `showDeleteAllCompletedConfirmationDialog` from `R.string.delete` to `R.string.delete_all_past_games_confirm`.

### Resources

#### [MODIFY] [strings.xml (English)](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/res/values/strings.xml)
- Add `<string name="delete_all_past_games_confirm">Delete All Games</string>`.

#### [MODIFY] [strings.xml (German)](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/res/values-de/strings.xml)
- Verify and ensure `<string name="delete_all_past_games_confirm">Alle Spiele löschen</string>` is present and correctly named.

### Configuration

#### [MODIFY] [build.gradle.kts (mobile)](file:///C:/Users/Jan/Downloads/RefWatch/mobile/build.gradle.kts)
- Update `versionName` to `"1.6.2"`.
- Increment `versionCode` to `361160200`.

#### [MODIFY] [build.gradle.kts (wear)](file:///C:/Users/Jan/Downloads/RefWatch/wear/build.gradle.kts)
- Update `versionName` to `"1.6.2"`.
- Increment `versionCode` to `361160200`.

## Verification Plan

### Automated Tests
- Build the `:mobile` module to ensure no resource errors or compilation issues.
  `./gradlew :mobile:assembleDebug`

### Manual Verification
- Verify the "Delete all past games" dialog in the app:
  - Button should say "Alle Spiele löschen" in German.
  - Button should say "Delete All Games" in English.
- Check the app info to verify version `1.6.2`.
