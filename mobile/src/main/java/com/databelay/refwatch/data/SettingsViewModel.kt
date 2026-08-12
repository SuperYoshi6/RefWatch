package com.databelay.refwatch.data

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.auth.AuthRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.functions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _pairingMessage = MutableStateFlow<String?>(null)
    val pairingMessage: StateFlow<String?> = _pairingMessage.asStateFlow()

    private val _isPairingLoading = MutableStateFlow(false)
    val isPairingLoading: StateFlow<Boolean> = _isPairingLoading.asStateFlow()

    private val _linkedDevices = MutableStateFlow<List<LinkedDevice>>(emptyList())
    val linkedDevices: StateFlow<List<LinkedDevice>> = _linkedDevices.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()

    var isDfbNetEnabled: Boolean
        get() = preferencesManager.isDfbNetEnabled
        set(value) {
            preferencesManager.isDfbNetEnabled = value
        }

    init {
        fetchLinkedDevices()
    }

    private fun fetchLinkedDevices() {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).collection("devices").get().await()
                val devices = snapshot.documents.map { doc ->
                    LinkedDevice(
                        id = doc.id,
                        linkedAt = (doc.getTimestamp("linkedAt")?.toDate()?.time) ?: 0L
                    )
                }
                _linkedDevices.value = devices
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error fetching devices", e)
            }
        }
    }

    fun pairWatch(code: String) {
        if (code.length != 6) {
            _pairingMessage.value = "Ungültiger Code. Bitte 6 Ziffern eingeben."
            return
        }

        viewModelScope.launch {
            _isPairingLoading.value = true
            _pairingMessage.value = null
            try {
                // Use default region first, but catch if it fails to suggest checking the deployment
                Firebase.functions
                    .getHttpsCallable("linkWatch")
                    .call(mapOf("code" to code))
                    .await()
                
                _pairingMessage.value = "Uhr erfolgreich verknüpft!"
                fetchLinkedDevices()
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Unbekannter Fehler"
                if (msg.contains("NOT_FOUND", ignoreCase = true) || msg.contains("404")) {
                    _pairingMessage.value = "Fehler: Server-Funktion nicht gefunden. Hast du 'firebase deploy' ausgeführt?"
                } else {
                    _pairingMessage.value = "Fehler: $msg"
                }
                Log.e("SettingsViewModel", "Pairing failed", e)
            } finally {
                _isPairingLoading.value = false
            }
        }
    }

    fun clearPairingMessage() {
        _pairingMessage.value = null
    }

    fun removeDevice(deviceId: String) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).collection("devices").document(deviceId).delete().await()
                fetchLinkedDevices()
                _pairingMessage.value = "Gerät erfolgreich entfernt."
            } catch (e: Exception) {
                _pairingMessage.value = "Fehler beim Entfernen: ${e.localizedMessage}"
            }
        }
    }
}

data class LinkedDevice(
    val id: String,
    val linkedAt: Long
)
