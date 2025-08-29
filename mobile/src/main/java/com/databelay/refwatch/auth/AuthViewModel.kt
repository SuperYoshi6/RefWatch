package com.databelay.refwatch.auth // Or your package

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.common.WearSyncConstants
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application, // Hilt provides this
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth // Inject FirebaseAuth
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    private val _isLoading = MutableStateFlow(true)
    private val _authError = MutableStateFlow<String?>(null)

    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val authError: StateFlow<String?> = _authError.asStateFlow()


    private val firebaseAuthListener = FirebaseAuth.AuthStateListener { auth ->
        val firebaseUser = auth.currentUser
        Log.d(TAG, "AuthStateListener triggered. User from auth: ${firebaseUser?.uid}")

        if (firebaseUser != null) {
            _currentUser.value = firebaseUser
            _authState.value = AuthState.Authenticated(firebaseUser)
            // Call function to handle token fetching and sending
            fetchCustomTokenAndSendDataToWatch(firebaseUser)
        } else {
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
            // Send nulls to indicate logout
            sendAuthDataToWatch(null, null)
        }
        _isLoading.value = false
    }

    init {
        firebaseAuth.addAuthStateListener(firebaseAuthListener)
        Log.d(TAG, "AuthViewModel initialized.")
        viewModelScope.launch {
            // Observe the repository's currentUserFlow to update internal AuthState
            authRepository.currentUserFlow.collect { user ->
                Log.d(TAG, "Collected user from repository: ${user?.uid}")
                 // The AuthStateListener will primarily handle _authState updates based on firebaseAuth.currentUser
                // This collector mainly ensures _isLoading is managed if the repository emits first.
                if (_authState.value is AuthState.Loading && user == null) {
                     _authState.value = AuthState.Unauthenticated // Set explicitly if still loading and repo says no user
                }
                // Only set isLoading to false here if the auth state hasn't already been resolved by the listener
                if (_isLoading.value && (_authState.value !is AuthState.Authenticated && _authState.value !is AuthState.Unauthenticated)) {
                    _isLoading.value = false
                }
            }
        }
        // Initial loading state will be set to false once the first emission from currentUserFlow arrives
        // or the AuthStateListener fires.
        viewModelScope.launch {
            val initialRepoUser = authRepository.currentUserFlow.value // Check initial value from repo
            val initialAuthUser = firebaseAuth.currentUser
            if (initialRepoUser == null && initialAuthUser == null) {
                _isLoading.value = false // if no user from either source, and we are still loading, stop.
            }
        }
    }

    private fun fetchCustomTokenAndSendDataToWatch(user: FirebaseUser) {
        viewModelScope.launch {
            Log.d(TAG, "User ${user.uid} authenticated. Attempting to get REAL custom token via Cloud Function.")

            val customTokenResult = authRepository.fetchCustomTokenFromServer() // Call the repository

            customTokenResult.onSuccess { customToken ->
                Log.i(TAG, "Successfully fetched custom token from server.")
                sendAuthDataToWatch(user.uid, customToken)
            }.onFailure { exception ->
                Log.e(TAG, "Failed to fetch custom token from server", exception)
                // Handle the error appropriately - maybe show an error message to the user
                // or retry. For now, we won't send anything to the watch if token fetching fails.
                _authError.value = "Failed to prepare watch sign-in: ${exception.localizedMessage}"
                // Optionally, you could still try to send a "login without token" signal
                // or a specific error signal to the watch if that's part of your design.
            }
        }
    }

    private fun sendAuthDataToWatch(userId: String?, customToken: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataClient = Wearable.getDataClient(application)
                val putDataMapReq = PutDataMapRequest.create(WearSyncConstants.PATH_PHONE_USER_ID)

                if (userId != null && customToken != null) {
                    putDataMapReq.dataMap.putString(WearSyncConstants.KEY_USER_ID, userId)
                    putDataMapReq.dataMap.putString(WearSyncConstants.KEY_CUSTOM_AUTH_TOKEN, customToken)
                    Log.i(TAG, "Preparing to send User ID ('$userId') and Custom Token (length: ${customToken.length}) to watch.")
                } else {
                    Log.i(TAG, "Preparing to send logout signal (empty DataMap for user path) to watch.")
                    // For logout, ensure dataMap is empty or explicitly cleared if needed by watch
                    // Sending an empty map for the path is a common way to signal cleared state.
                }

                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                val dataItem = dataClient.putDataItem(putDataReq).await()
                Log.i(TAG, "Successfully sent auth data (DataItem: ${dataItem.uri}) to watch. UserID: '$userId'")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send auth data (UserID: '$userId') to watch.", e)
            }
        }
    }

    fun signInWithEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Email and password cannot be empty."
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            _authError.value = null
            val result = authRepository.signInWithEmailPassword(email, password)
            result.onSuccess {
                // AuthStateListener will handle Authenticated state and triggering data send to watch
                Log.d(TAG, "Sign-in successful via repository.")
            }.onFailure { e ->
                Log.e(TAG, "Sign-in failed via repository", e)
                val errorMessage = e.message ?: "Sign-in failed."
                _authError.value = errorMessage
                _authState.value = AuthState.Error(errorMessage)
                _isLoading.value = false // Ensure loading is stopped on failure
            }
        }
    }

    fun signUpWithEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            val errorMsg = "Email and password cannot be empty."
            _authError.value = errorMsg
            _isLoading.value = false
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            _authError.value = null
            try {
                // authRepository.signUpWithEmailPassword internally calls Firebase
                // The AuthStateListener will detect the new user and trigger data send.
                authRepository.signUpWithEmailPassword(email, password)
                Log.d(TAG, "Firebase signUpWithEmailAndPassword task initiated via repository.")
            } catch (e: Exception) { // Catching from repository call if it throws directly
                Log.e(TAG, "Sign-up initiation failed", e)
                val errorMessage = e.message ?: "Sign-up failed. Please try again."
                _authError.value = errorMessage
                _authState.value = AuthState.Error(errorMessage)
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        Log.d(TAG, "signOut called.")
        _authError.value = null
        authRepository.signOut()
        // AuthStateListener will handle Unauthenticated state and triggering data send to watch.
        // No need to set _isLoading to true for a local signOut operation.
        // Listener will set _isLoading to false.
    }

    fun clearAuthError() {
        _authError.value = null
        if (_authState.value is AuthState.Error) {
            if (_currentUser.value != null) {
                _authState.value = AuthState.Authenticated(_currentUser.value!!)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(firebaseAuthListener) // Clean up listener
        Log.d(TAG, "ViewModel cleared, AuthStateListener removed.")
    }
    fun deleteUserAccount() {
        viewModelScope.launch {
            _isLoading.value = true // Show loading state
            val user = firebaseAuth.currentUser
            user?.delete()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User account deleted successfully from Firebase Auth.")
                    // Now delete associated data from Firestore/backend
                    // This might involve another suspend function call
                    // clearUserAppData(user.uid) // Example

                    // AuthStateListener will eventually pick up the null user
                    // and set _authState to Unauthenticated.
                    // Or you can explicitly set it if needed, though listener is better.
                    _isLoading.value = false
                    // _deleteSuccessEvent.value = true // Or use a SingleLiveEvent / SharedFlow for navigation trigger
                } else {
                    Log.e(TAG, "Failed to delete user account from Firebase Auth.", task.exception)
                    _authError.value = "Failed to delete account: ${task.exception?.message}"
                    _isLoading.value = false
                }
            } ?: run {
                _authError.value = "No user found to delete."
                _isLoading.value = false
            }
        }
    }

}
