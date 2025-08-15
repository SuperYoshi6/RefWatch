package com.databelay.refwatch.wear.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    private val tag = "WatchAuthManager"

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val authScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Tracks the watch's own Firebase authenticated user
    val currentWatchUserId: StateFlow<String?> = firebaseUser.map { it?.uid }
        .stateIn(
            scope = authScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = firebaseAuth.currentUser?.uid
        )

    // Tracks the User ID received from the paired phone
    private val _currentPhoneUserId = MutableStateFlow<String?>(null)
    val currentPhoneUserId: StateFlow<String?> = _currentPhoneUserId.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _firebaseUser.value = user
            Log.i(tag, "Firebase AuthState changed. Watch User: ${user?.uid ?: "Not signed in"}")
        }
        Log.d(tag, "Initialized. Current Watch Firebase User: ${firebaseAuth.currentUser?.uid ?: "Not signed in"}")
        // We don't have an initial value for phone user ID other than null
        Log.d(tag, "Initial Phone User ID: ${_currentPhoneUserId.value}")
    }

    fun signInWithCustomToken(customToken: String) {
        if (customToken.isBlank()) {
            Log.w(tag, "signInWithCustomToken called with blank token.")
            return
        }
        authScope.launch {
            try {
                Log.d(tag, "Attempting to sign in with custom token.")
                val authResult = firebaseAuth.signInWithCustomToken(customToken).await()
                Log.i(tag, "Successfully signed in with custom token. Watch User: ${authResult.user?.uid}")
            } catch (e: Exception) {
                Log.e(tag, "Error signing in with custom token", e)
            }
        }
    }

    fun signOut() {
        authScope.launch {
            val currentUid = _firebaseUser.value?.uid
            if (currentUid != null) {
                Log.i(tag, "Signing out current watch user: $currentUid")
                firebaseAuth.signOut()
                Log.i(tag, "Sign out initiated for watch user $currentUid.")
            } else {
                Log.i(tag, "No watch user to sign out.")
            }
            // Also clear the phone user ID when the watch user signs out,
            // as they are typically linked or the context is lost.
            // Alternatively, this could be handled by a new DataItem from the phone.
            if (_currentPhoneUserId.value != null) {
                Log.i(tag, "Clearing phone user ID as watch user signed out.")
                updateCurrentPhoneUserId(null)
            }
        }
    }

    /**
     * Updates the understood User ID of the currently active user on the paired phone.
     * This is typically called by WearDataListenerService when it receives an update from the phone.
     */
    fun updateCurrentPhoneUserId(phoneUserId: String?) {
        if (_currentPhoneUserId.value != phoneUserId) {
            _currentPhoneUserId.value = phoneUserId
            Log.i(tag, "Phone User ID updated to: $phoneUserId")
        } else {
            Log.d(tag, "Phone User ID received ($phoneUserId) is same as current. No update.")
        }
    }
}
