// In a new file, e.g., auth/AuthRepository.kt
package com.databelay.refwatch.auth

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.functions.functions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

@Singleton // This repository can be a singleton
class AuthRepository @Inject constructor(private val firebaseAuth: FirebaseAuth) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    suspend fun fetchCustomTokenFromServer(): Result<String> {
        // Allow public access on google cloud console
        // https://console.cloud.google.com/run/detail/us-central1/generatecustomtoken/security


        val currentUser = firebaseAuth.currentUser
        Log.d(TAG, "Attempting to call generateCustomToken. Current user: ${currentUser?.uid}, Email: ${currentUser?.email}")

        if (currentUser == null) {
            Log.e(TAG, "User is NULL. Cannot call generateCustomToken without authentication.")
            return Result.failure(Exception("User not authenticated for Cloud Function call"))
        }

        // *** ADD THIS BLOCK TO FORCE REFRESH THE TOKEN ***
        try {
            Log.d(TAG, "Attempting to refresh ID token...")
            val tokenResult = currentUser.getIdToken(true).await() // true forces refresh
            if (tokenResult?.token == null) {
                Log.e(TAG, "ID token is null after refresh attempt.")
                return Result.failure(Exception("Failed to refresh ID token (token is null) before Cloud Function call"))
            }
            Log.i(TAG, "ID token refreshed successfully. Token starts with: ${tokenResult.token?.take(10)}...")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh ID token", e)
            return Result.failure(Exception("Failed to refresh ID token before Cloud Function call", e))
        }
        // *** END OF ADDED BLOCK ***

        return try {
            // If you injected FirebaseFunctions as 'functions', use it:
            // val result = functions
            // If not, use the Firebase.functions singleton:
            val result = Firebase.functions // Ensure Firebase.functions is correctly set up if not using emulator.
                .getHttpsCallable("generateCustomToken")
                .call()
                .await()

            val data = result.data as? Map<*, *>
            val customToken = data?.get("customToken") as? String

            if (customToken != null) {
                Log.i(TAG, "Successfully fetched custom token of length: ${customToken.length}")
                Result.success(customToken)
            } else {
                Log.e(TAG, "Custom token from server is null or not a string. Data received: ${result.data}")
                Result.failure(Exception("Failed to parse custom token from server response."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling generateCustomToken Cloud Function", e)
            Result.failure(e)
        }
    }
    // Create a scope for the repository if you don't have a global one
    // Or inject one if you use Hilt and define a global scope module
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Exposes a Flow of the current FirebaseUser
    val currentUserFlow: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            Log.d(TAG, "AuthStateListener in Repository: User: ${auth.currentUser?.uid}")
            trySend(auth.currentUser).isSuccess
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            Log.d(TAG, "AuthStateListener in Repository: Removing listener.")
            firebaseAuth.removeAuthStateListener(listener)
        }
    }.stateIn( // Convert the callbackFlow to a StateFlow
        scope = repositoryScope, // Provide a CoroutineScope
        started = SharingStarted.WhileSubscribed(5000L), // Or Lazily, Eagerly
        initialValue = firebaseAuth.currentUser // Provide an initial value
    )

    // You can also add suspend functions for signIn, signOut, signUp here
    suspend fun signInWithEmailPassword(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(authResult.user!!) // Assuming user is not null on success
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmailPassword(email: String, pass: String, displayName: String? = null): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting sign-up for email: $email")
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user
            if (firebaseUser == null) {
                Log.e(TAG, "Sign-up failed for email: $email - FirebaseUser is null after creation.")
                return Result.failure(Exception("Sign-up failed: User not created."))
            }

            Log.i(TAG, "Sign-up successful for email: $email, UID: ${firebaseUser.uid}")

            // Optionally, update the user's display name if provided
            if (displayName != null && displayName.isNotBlank()) {
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                    Log.i(TAG, "Display name '$displayName' set for user: ${firebaseUser.uid}")
                } catch (profileUpdateException: Exception) {
                    Log.w(TAG, "Sign-up successful, but failed to set display name for user: ${firebaseUser.uid}", profileUpdateException)
                    // Continue, as sign-up itself was successful
                }
            }
            // The AuthStateListener in currentUserFlow will eventually emit this new user.
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-up failed for email: $email", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
    // You might want to add other methods like:
    // suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    // suspend fun reauthenticateUser(credential: AuthCredential): Result<Unit>
    // suspend fun deleteUser(): Result<Unit>
}