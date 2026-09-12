package com.databelay.refwatch.common

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.UUID
import kotlinx.serialization.modules.SerializersModule

val gameEventModule = SerializersModule {
    polymorphic(GameEvent::class) {
        subclass(GoalScoredEvent::class)
        subclass(PenaltyEvent::class)
        subclass(CardIssuedEvent::class)
        subclass(SubstitutionEvent::class)
        subclass(TemporaryDismissalEvent::class)
        subclass(PhaseChangedEvent::class)
        subclass(GenericLogEvent::class)
    }
}

@Serializable
@Immutable
enum class GoalType {
    OPEN_PLAY,           // Feldtor
    PENALTY,             // Strafstoß
    OWN_GOAL,            // Eigentor
    REGULAR              // Fallback für Legacy-Daten
}

// --- Game Event data Class and its Subclasses ---
@Serializable
sealed class GameEvent : Parcelable {
    abstract val id: String
    abstract val timestamp: Double // Wall-clock time of event logging
    abstract val gameTimeMillis: Double // Game clock time when event occurred
    abstract val displayString: String // User-friendly string for the log
    abstract val phase: GamePhase? // The phase during which the event occurred
}

@Serializable
@SerialName("GOAL")
@Parcelize
@Immutable
data class GoalScoredEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val teamDisplayName: String? = null,
    val goalType: GoalType = GoalType.REGULAR,
    val playerNumber: Int? = null,
    val assistantNumber: Int? = null,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    val homeScoreAtTime: Int,
    val awayScoreAtTime: Int,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
    get() {
        val playerStr = if (playerNumber != null) " Nr. $playerNumber" else ""
        val assistantStr = if (assistantNumber != null) " (Assist: Nr. $assistantNumber)" else ""
        val teamStr = teamDisplayName?.takeIf { it.isNotBlank() } ?: team.name
        return "Tor $teamStr$playerStr$assistantStr (${homeScoreAtTime}:${awayScoreAtTime})"
    }
}

@Serializable
@SerialName("PENALTY")
@Parcelize
@Immutable
data class PenaltyEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    /**
     * Jersey number of the player who took the kick. Only set for kicks that
     * occur during a penalty shootout (phase == PENALTIES); left null for
     * in-game penalty kicks where the player number is not normally recorded.
     *
     * `kickerNumber` is intentionally optional so existing Firestore data
     * (where this field is missing) still deserializes correctly via the
     * default value.
     */
    val kickerNumber: Int? = null,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    val homeScoreAtTime: Int,
    val awayScoreAtTime: Int,
    val scored: Boolean,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() {
            val outcome = if (scored) "GETROFFEN" else "VERGEBEN/GEHALTEN"
            val kicker = if (kickerNumber != null) " Nr. $kickerNumber" else ""
            return "Elfmeter: ${team.name}$kicker $outcome"
        }
}


@Serializable
@SerialName("CARD")
@Parcelize
@Immutable
data class CardIssuedEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val teamDisplayName: String? = null,
    val playerNumber: Int,
    val cardType: CardType,
    val isOfficial: Boolean = false,
    val officialName: String? = null,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() {
            val typeStr = when (cardType) {
                CardType.YELLOW -> "Gelbe"
                CardType.RED -> "Rote"
            }
            val teamStr = teamDisplayName?.takeIf { it.isNotBlank() } ?: team.name
            return if (isOfficial) {
                "$typeStr Karte (${officialName ?: "Offizieller"} $teamStr)"
            } else {
                "$typeStr Karte: $teamStr Nr. $playerNumber"
            }
        }
}

@Serializable
@SerialName("PHASE_CHANGE")
@Parcelize
@Immutable
data class PhaseChangedEvent(
    override val id: String = UUID.randomUUID().toString(),
    val newPhase: GamePhase,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() = newPhase.readable()
}

@Serializable
@SerialName("GENERIC_LOG")
@Parcelize
@Immutable
data class GenericLogEvent(
    override val id: String = UUID.randomUUID().toString(),
    val message: String,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double = 0.0,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() = message
}

@Serializable
@SerialName("SUBSTITUTION")
@Parcelize
@Immutable
data class SubstitutionEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val teamDisplayName: String? = null,
    val outgoingPlayerNumber: Int,
    val incomingPlayerNumber: Int,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() {
            val name = teamDisplayName?.takeIf { it.isNotBlank() } ?: team.name
            return "Wechsel $name: $outgoingPlayerNumber ➡️ $incomingPlayerNumber"
        }
}

@Serializable
@SerialName("TEMPORARY_DISMISSAL")
@Parcelize
@Immutable
data class TemporaryDismissalEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val teamDisplayName: String? = null,
    val playerNumber: Int,
    val durationMinutes: Int,
    val startMatchTimeMillis: Double, // Match time when it started
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() {
            val teamStr = teamDisplayName?.takeIf { it.isNotBlank() } ?: team.name
            return "Zeitstrafe $teamStr Nr. $playerNumber ($durationMinutes min)"
        }
}
