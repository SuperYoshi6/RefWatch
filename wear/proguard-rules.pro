# ============================================================================
# RefWatch Wear OS — R8 / ProGuard rules
# ============================================================================
# These rules are required because several libraries we use rely on reflection
# or code generation. Without them, R8 will strip classes that the runtime
# still needs and the app will crash with NoSuchMethodError / ClassNotFound
# exceptions in release builds. Keep this file in sync with the dependencies
# in wear/build.gradle.kts.
# ============================================================================

# ---------------------------------------------------------------------------
# Kotlinx Serialization
# ---------------------------------------------------------------------------
# The @Serializable annotation processor generates a `*$Companion` class with
# a `serializer()` method, plus a `$$serializer` synthetic class. R8 must
# preserve both, otherwise Firestore (de)serialization will fail with
# "Serializer for class X is not found".
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.databelay.refwatch.common.**$$serializer { *; }
-keepclassmembers class com.databelay.refwatch.common.** {
    *** Companion;
}
-keepclasseswithmembers class com.databelay.refwatch.common.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Polymorphic GameEvent dispatch table — the sealed-class serializer is
# looked up by class name at runtime.
-keep class com.databelay.refwatch.common.GameEvent { *; }
-keep class com.databelay.refwatch.common.GameEvent$* { *; }
-keep class com.databelay.refwatch.common.AgeGroup { *; }
-keep class com.databelay.refwatch.common.AgeGroup$* { *; }
-keep class com.databelay.refwatch.common.GamePhase { *; }
-keep class com.databelay.refwatch.common.GamePhase$* { *; }
-keep class com.databelay.refwatch.common.Team { *; }
-keep class com.databelay.refwatch.common.Team$* { *; }
-keep class com.databelay.refwatch.common.GameStatus { *; }
-keep class com.databelay.refwatch.common.GameStatus$* { *; }
-keep class com.databelay.refwatch.common.GoalType { *; }
-keep class com.databelay.refwatch.common.GoalType$* { *; }
-keep class com.databelay.refwatch.common.CardType { *; }
-keep class com.databelay.refwatch.common.CardType$* { *; }

# ---------------------------------------------------------------------------
# Firestore / Firebase
# ---------------------------------------------------------------------------
# POJOs deserialized via Firestore's toObject() use field-name reflection
# (or @PropertyName). R8 must keep all field names for the @IgnoreExtra
# Properties model classes. We use keepclassmembers so R8 can still rename
# the class itself but the field names survive.
-keep class com.databelay.refwatch.common.Game {
    <fields>;
    <init>(...);
}
-keep class com.databelay.refwatch.common.Player {
    <fields>;
    <init>(...);
}
-keep class com.databelay.refwatch.common.Game$* { *; }
-keep class com.databelay.refwatch.common.Player$* { *; }

# Standard Firebase recommendations (covers Firestore and Auth runtime).
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ---------------------------------------------------------------------------
# Hilt / Dagger
# ---------------------------------------------------------------------------
# Hilt-generated *_HiltModules, *_Factory and the application class are
# referenced by reflection at startup. Keep them; the Hilt plugin would
# normally inject these rules, but enabling R8 without them sometimes still
# sheds generated metadata.
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

# ---------------------------------------------------------------------------
# Wearable Data Layer (WearableListenerService is referenced by manifest name
# AND instantiated by GMS, so R8 must not strip the class file even if it
# sees no in-code reference).
# ---------------------------------------------------------------------------
-keep class com.databelay.refwatch.wear.data.GameTimerService { *; }
-keep class com.databelay.refwatch.wear.data.WearDataListenerService { *; }
-keep class com.google.android.gms.wearable.** { *; }

# ---------------------------------------------------------------------------
# Compose
# ---------------------------------------------------------------------------
# Compose 1.4+ ships its own consumer rules, but the Wear-specific `androidx.
# wear.compose` packages are not always bundled. Defensive keep for the
# pager and material3 packages.
-keep class androidx.wear.compose.foundation.pager.** { *; }
-keep class androidx.wear.compose.material3.** { *; }
-keep class androidx.wear.compose.foundation.** { *; }

# ---------------------------------------------------------------------------
# Misc
# ---------------------------------------------------------------------------
# Keep line numbers (helps with stack-trace deobfuscation from the field).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
