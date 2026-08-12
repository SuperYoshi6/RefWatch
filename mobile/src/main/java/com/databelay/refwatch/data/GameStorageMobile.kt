package com.databelay.refwatch.data

// import com.google.gson.Gson // No longer needed for event parsing if using ktx.serialization consistently

import android.util.Log
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.parseGameEventsFromDocument
import com.databelay.refwatch.common.toFirestoreMap
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.collections.iterator


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

    suspend fun getGameById(userId: String, gameId: String): Game? {
        if (userId.isBlank() || gameId.isBlank()) {
            Log.w(tag, "getGameById: userId or gameId is blank. userId=$userId, gameId=$gameId")
            return null
        }

        return try {
            val docSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(GAMES_COLLECTION)
                .document(gameId)
                .get()
                .await()

            if (!docSnapshot.exists()) {
                Log.w(tag, "getGameById: No game found with ID $gameId for user $userId.")
                return null
            }

            val gameBase = docSnapshot.toObject<Game>()
            if (gameBase == null) {
                Log.w(tag, "getGameById: Failed to convert document $gameId to Game for user $userId.")
                return null
            }

            gameBase.copy(
                id = docSnapshot.id,
                events = parseGameEventsFromDocument(docSnapshot)
            )
        } catch (ex: Exception) {
            Log.e(tag, "getGameById: Error fetching game $gameId for user $userId", ex)
            null
        }
    }

    suspend fun addOrUpdateGame(userId: String, gameToSave: Game): Result<Unit> {
        Log.d(tag, "addOrUpdateGame: User: $userId, Game ID: ${gameToSave.id}")
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }

        return try {
            val gameDocumentRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(GAMES_COLLECTION)
                .document(gameToSave.id)

            val dataToSet = gameToSave.toFirestoreMap()
            
            // Use set with SetOptions.merge() to perform an UPSERT.
            // This creates the document if it doesn't exist, or merges fields if it does.
            // This avoids the need for a prior 'get()' which can fail due to security rules or network issues.
            gameDocumentRef.set(dataToSet, SetOptions.merge()).await()

            Log.i(tag, "Successfully saved/updated game ${gameToSave.id} for user $userId.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving game ${gameToSave.id} for user $userId: ${e.message}", e)
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

