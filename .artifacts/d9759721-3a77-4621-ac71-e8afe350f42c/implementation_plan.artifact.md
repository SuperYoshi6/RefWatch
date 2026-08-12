# Fix Persistent Save Failure (Firestore Permissions & Logic)

The user reports a "Failed to get document" error when saving. This indicates that the app is trying to read from Firestore before writing, but the request is being rejected. This is common when Firestore security rules are not yet configured for a new project or when the `userId` path is inaccessible.

## User Review Required

> [!CAUTION]
> **Action Required**: Since we switched to a new Firebase project (`refwatch-1938a`), you **must** ensure that the Firestore database is initialized and the rules allow you to write.
> 1. Go to the [Firebase Console](https://console.firebase.google.com/).
> 2. Click on **Firestore Database**.
> 3. If you haven't created it yet, click **Create database**.
> 4. Go to the **Rules** tab and ensure they are set to "Test mode" (for testing) or correctly partitioned by `userId`.
>    - **Recommendation for testing**:
>      ```
>      service cloud.firestore {
>        match /databases/{database}/documents {
>          match /users/{userId}/games/{gameId} {
>            allow read, write: if request.auth != null && request.auth.uid == userId;
>          }
>        }
>      }
>      ```

## Proposed Changes

### 1. Simplify Save Logic

#### [MODIFY] [GameStorageMobile.kt](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/java/com/databelay/refwatch/data/GameStorageMobile.kt)
- **Refactor `addOrUpdateGame`**:
    - Remove the `get().await()` calls (Attempt 1 and 2).
    - Use `set(data, SetOptions.merge())` instead. This performs an "Upsert" (Update if exists, Create if not) in a single call without requiring a prior read.
    - This bypasses the "Failed to get document" error entirely and reduces network latency.
    - Simplified preservation logic: Only essential fields (like `id` and `userId`) are enforced; the rest is merged.

### 2. Robust Data Preparation

#### [MODIFY] [Game.kt](file:///C:/Users/Jan/Downloads/RefWatch/common/src/main/java/com/databelay/refwatch/common/Game.kt)
- Ensure `toFirestoreMap` uses `SetOptions.merge()` compatible keys.
- Ensure enums and lists are always converted to basic Firestore types (Strings/Maps/Lists).

### 3. Better Error Feedback

#### [MODIFY] [AddEditGameViewModel.kt](file:///C:/Users/Jan/Downloads/RefWatch/mobile/src/main/java/com/databelay/refwatch/data/AddEditGameViewModel.kt)
- Add a specific check to verify if the user's email is verified (optional, but good for diagnostics).
- Log the exact exception type and message to help distinguish between "Permission Denied" and "Network Error".

## Verification Plan

### Manual Verification
1.  **Check Firebase Console**: Verify Firestore is enabled and rules are not "Locked".
2.  **Save Game**:
    - Open "Add Game".
    - Tap **Save**.
    - Verify that the game is saved directly via `set(merge: true)`.
    - Verify no "Failed to get document" error occurs because the app no longer tries to "get" the document first.
