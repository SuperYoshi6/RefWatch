package com.databelay.refwatch.common

import android.os.Parcelable
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
        subclass(PhaseChangedEvent::class)
        subclass(GenericLogEvent::class)
    }
}

@Serializable
enum class GoalType {
    REGULAR,
    PENALTY,
    OWN_GOAL
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
data class GoalScoredEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val goalType: GoalType = GoalType.REGULAR,
    val playerNumber: Int? = null,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    val homeScoreAtTime: Int,
    val awayScoreAtTime: Int,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
    get() {
        val typeStr = when (goalType) {
            GoalType.REGULAR -> "Goal"
            GoalType.PENALTY -> "Penalty Goal"
            GoalType.OWN_GOAL -> "Own Goal"
        }
        val playerStr = if (playerNumber != null) " (Player #$playerNumber)" else ""
        return "$typeStr$playerStr: ${team.name} ($homeScoreAtTime-$awayScoreAtTime) at ${
            gameTimeMillis.toLong().formatTime()
        }"
    }
}

@Serializable
@SerialName("PENALTY")
@Parcelize
data class PenaltyEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    val homeScoreAtTime: Int,
    val awayScoreAtTime: Int,
    val scored: Boolean,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() = "Penalty: ${team.name} ${if (scored) "SCORED" else "MISSED/SAVED"}"
}


@Serializable
@SerialName("CARD")
@Parcelize
data class CardIssuedEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val playerNumber: Int,
    val cardType: CardType,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() = "${cardType.name.replaceFirstChar { it.uppercase() }} Card: ${team.name}, Player #$playerNumber at ${
            gameTimeMillis.toLong().formatTime()
        }"
}

@Serializable
@SerialName("PHASE_CHANGE")
@Parcelize
data class PhaseChangedEvent(
    override val id: String = UUID.randomUUID().toString(),
    val newPhase: GamePhase,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() = "${newPhase.readable()} (Clock: ${gameTimeMillis.toLong().formatTime()})"
}

@Serializable
@SerialName("GENERIC_LOG")
@Parcelize
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
data class SubstitutionEvent(
    override val id: String = UUID.randomUUID().toString(),
    val team: Team,
    val outgoingPlayerNumber: Int,
    val incomingPlayerNumber: Int,
    override val timestamp: Double = System.currentTimeMillis().toDouble(),
    override val gameTimeMillis: Double,
    override val phase: GamePhase? = null
) : GameEvent() {
    @get:Exclude
    override val displayString: String
        get() = "Wechsel: ${team.name}, #$outgoingPlayerNumber ➡️ #$incomingPlayerNumber at ${
            gameTimeMillis.toLong().formatTime()
        }"
}
