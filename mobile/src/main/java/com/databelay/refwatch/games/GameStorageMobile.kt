package com.databelay.refwatch.games

import android.util.Log
import com.databelay.refwatch.common.AppJsonConfiguration
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GameEvent
import com.databelay.refwatch.common.mapToJsonObject
import com.databelay.refwatch.common.toFirestoreMap

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
// import com.google.gson.Gson // No longer needed for event parsing if using ktx.serialization consistently
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import kotlinx.serialization.json.decodeFromJsonElement


class GameStorageMobile(private val firestore: FirebaseFirestore) {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val GAMES_COLLECTION = "games"
        private const val tag = "GameRepository"
    }

    fun getGamesFlow(userId: String): Flow<List<Game>> {
        if (userId.isBlank()) {
            Log.w(tag, "getGamesFlow: userId is blank. Returning empty flow.")
            return callbackFlow { trySend(emptyList()); awaitClose { } } // Or handle as an error
        }

        Log.d(tag, "getGamesFlow: Setting up listener for user $userId")
        val gamesCollectionRef = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(GAMES_COLLECTION)
            .orderBy("gameDateTimeEpochMillis", Query.Direction.ASCENDING) // Or another relevant field

        return callbackFlow {
            val listenerRegistration = gamesCollectionRef.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w(tag, "getGamesFlow: Listen failed for user $userId.", e)
                    close(e) // Close the flow with an error
                    return@addSnapshotListener
                }

                if (snapshots == null) {
                    Log.d(tag, "getGamesFlow: Snapshots object is null for user $userId. Sending empty list.")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                Log.d(tag, "getGamesFlow: Snapshot received. Number of documents: ${snapshots.size()} for user $userId.")

                val gamesList = snapshots.documents.mapNotNull { document ->
                    try {
                        // 1. Convert to Game object.
                        //    If Game.events has @Exclude, this 'gameBase' will have an empty events list.
                        val gameBase = document.toObject<Game>()

                        if (gameBase == null) {
                            Log.w(tag, "getGamesFlow: Failed to convert document ${document.id} to Game for user $userId. Skipping.")
                            return@mapNotNull null
                        }

                        // 2. Manually parse the events from the document data
                        val parsedEvents = parseGameEventsFromDocument(document)
                        Log.v(tag, "getGamesFlow: Parsed ${parsedEvents.size} events for game ${document.id}")


                        // 3. Return a new Game object with the manually parsed events
                        //    and ensure the Firestore document ID is used.
                        gameBase.copy(
                            id = document.id, // Ensure Firestore document ID is used as the game's ID
                            events = parsedEvents
                        )
                    } catch (ex: Exception) {
                        Log.e(tag, "getGamesFlow: Error converting document ${document.id} to Game for user $userId", ex)
                        null // Skip this document if there's an error
                    }
                }
                Log.d(tag, "getGamesFlow: Processed ${gamesList.size} games for user $userId after parsing events.")
                trySend(gamesList)
            }

            awaitClose {
                Log.d(tag, "getGamesFlow: Closing games flow listener for user $userId")
                listenerRegistration.remove()
            }
        }
    }


    // This function is now correctly placed and used by getGamesFlow
    private fun parseGameEventsFromDocument(document: DocumentSnapshot): List<GameEvent> {
        val eventMapsFirestore = document.get("events") as? List<Map<String, Any?>> ?: run {
            Log.d(tag, "parseGameEventsFromDocument: Document ${document.id} has no 'events' field, or it's not a list. Returning empty list.")
            return emptyList()
        }

        if (eventMapsFirestore.isEmpty()) {
            Log.d(tag, "parseGameEventsFromDocument: Document ${document.id} has an empty 'events' list (read from Firestore).")
            return emptyList()
        }
        Log.d(tag, "parseGameEventsFromDocument: Document ${document.id} has ${eventMapsFirestore.size} event maps to parse from Firestore.")

        return eventMapsFirestore.mapNotNull { eventMapFromFirestore ->
            // LOG 3: Log the individual event map from Firestore
            Log.v(tag, "parseGameEventsFromDocument: Attempting to decode event map for doc ${document.id}: $eventMapFromFirestore")
            try {
                // 1. Convert Map<String, Any?> from Firestore TO a kotlinx.serialization.json.JsonObject
                val jsonObject = mapToJsonObject(eventMapFromFirestore)
                // LOG 4: Log the JsonObject before ktx.serialization decodes it to GameEvent
                Log.v(tag, "parseGameEventsFromDocument: Converted Firestore map to JsonObject for doc ${document.id}: $jsonObject")

                // 2. Deserialize the JsonObject TO a GameEvent object
                // ktxJson needs classDiscriminator and SerializersModule configured to work here.
                // This also works if GameEvent is a sealed class and setup for polymorphism
                val event: GameEvent = AppJsonConfiguration.decodeFromJsonElement(jsonObject)
                // LOG 5: Log the successfully decoded event
                Log.d(tag, "parseGameEventsFromDocument: Successfully decoded event for doc ${document.id}: $event")
                event
            } catch (e: Exception) {
                // LOG 6: THIS IS VERY IMPORTANT IF EVENTS ARE MISSING or parsing fails
                Log.e(tag, "parseGameEventsFromDocument: Error decoding a single GameEvent from map for doc ${document.id}. Map from Firestore: $eventMapFromFirestore. Error: ", e)
                // Optionally log the JsonObject if the mapToJsonObject conversion seems problematic:
                // try { val problematicJson = mapToJsonObject(eventMapFromFirestore); Log.e(TAG, "Problematic JsonObject: $problematicJson") } catch (jsonEx: Exception) { Log.e(TAG, "Failed to convert map to JsonObject for error logging", jsonEx) }
                null
            }
        }
    }

    suspend fun addOrUpdateGame(userId: String, game: Game): Result<Unit> {
        Log.d(tag, "addOrUpdateGame: User: $userId, Game ID: ${game.id}, Events in Game object: ${game.events.size}")
        if (userId.isBlank()) {
            Log.e(tag, "addOrUpdateGame: userId is blank.")
            return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }
        if (game.id.isBlank()) {
            Log.e(tag, "addOrUpdateGame: game.id is blank for user $userId.")
            return Result.failure(IllegalArgumentException("Game ID cannot be blank for addOrUpdateGame"))
        }

        return try {
            val gameDocumentRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(GAMES_COLLECTION)
                .document(game.id) // Using game.id as the document ID

            // Use the extension function
            val gameDataForFirestore = game.toFirestoreMap() // Pass AppJsonConfiguration if needed

            Log.d(tag, "addOrUpdateGame (Wear): Saving game ${game.id} for user $userId with ${(gameDataForFirestore["events"] as? List<*>)?.size ?: 0} events.")
            Log.v(tag, "addOrUpdateGame: Data being sent to Firestore: $gameDataForFirestore")

            gameDocumentRef.set(gameDataForFirestore).await()
            Log.i(tag, "addOrUpdateGame: Successfully saved game ${game.id} to Firestore for user $userId.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "addOrUpdateGame: Error saving game ${game.id} for user $userId to Firestore. Error: ${e.message}", e)
            Result.failure(e)
        }

    }


    suspend fun deleteGame(userId: String, gameId: String): Result<Unit> {
        return try {
            if (userId.isEmpty()) return Result.failure(IllegalArgumentException("User ID cannot be empty"))
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(GAMES_COLLECTION)
                .document(gameId)
                .delete()
                .await()
            Log.d(tag, "Game $gameId deleted successfully for user $userId.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting game $gameId for user $userId", e)
            Result.failure(e)
        }
    }
}

