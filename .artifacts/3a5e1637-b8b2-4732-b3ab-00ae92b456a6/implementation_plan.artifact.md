# Auth Screen UI Improvements & Localization

This plan addresses the UI enhancements for the Auth screen, including a visual separator between login methods and full localization for English users.

## User Review Required

> [!NOTE]
> I will add a horizontal separator with the text "or" (or "oder") between the "Sign In" button and the "Google" button to make the distinction clearer.

## Proposed Changes

### 1. Localization (Resources)

#### [MODIFY] [strings.xml](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/res/values/strings.xml)
- Add `or_separator`: "or"
- Add `auth_tagline`: "Game Time · Goals · Cards · Substitutions"
- Add `auth_footer`: "Made with ❤️ for referees"

#### [MODIFY] [strings.xml (DE)](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/res/values-de/strings.xml)
- Add `or_separator`: "oder"
- Add `auth_tagline`: "Spielzeit · Tore · Karten · Wechsel"
- Add `auth_footer`: "Mit ❤️ für Schiedsrichter gemacht"

---

### 2. UI Components

#### [MODIFY] [AuthScreen.kt](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/java/com/databelay/refwatch/screens/AuthScreen.kt)
- Replace hardcoded tagline and footer strings with `stringResource`.
- **Add Separator**: Insert a `Row` between the primary "Sign In" button and the "Google" button containing:
    - A `HorizontalDivider` on the left.
    - The `or_separator` text in the middle.
    - A `HorizontalDivider` on the right.
- Ensure the Email login remains the primary focus (at the top of the button stack).

## Verification Plan

### Manual Verification
1.  **Language Check (DE)**: Set device to German. Verify tagline, footer, and separator show "Spielzeit...", "Mit ❤️...", and "oder".
2.  **Language Check (EN)**: Set device to English. Verify tagline, footer, and separator show "Game Time...", "Made with ❤️...", and "or".
3.  **Visual Check**: Ensure the "or" separator looks balanced and matches the app's dark theme.
