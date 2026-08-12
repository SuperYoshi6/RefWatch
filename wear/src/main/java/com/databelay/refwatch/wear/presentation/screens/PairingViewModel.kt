package com.databelay.refwatch.wear.presentation.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.wear.auth.WatchAuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class PairingUiState(
    val code: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val authManager: WatchAuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()
    
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private val watchId = UUID.randomUUID().toString()

    init {
        generateCode()
    }

    private fun generateCode() {
        viewModelScope.launch {
            try {
                val code = (100000..999999).random().toString()
                
                val pairingData = mapOf(
                    "watchId" to watchId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "status" to "pending"
                )
                
                firestore.collection("pairing_codes").document(code)
                    .set(pairingData)
                    .await()
                
                _uiState.value = PairingUiState(code = code, isLoading = false)
                
                startListening(code)
            } catch (e: Exception) {
                _uiState.value = PairingUiState(errorMessage = e.localizedMessage, isLoading = false)
            }
        }
    }

    private fun startListening(code: String) {
        listenerRegistration = firestore.collection("pairing_codes").document(code)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage)
                    return@addSnapshotListener
                }
                
                val token = snapshot?.getString("customToken")
                if (!token.isNullOrBlank()) {
                    authManager.signInWithCustomToken(token)
                    _uiState.value = _uiState.value.copy(isSuccess = true)
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
