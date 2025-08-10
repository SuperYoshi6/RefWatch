package com.databelay.refwatch.wear.data

import android.content.Intent
import android.util.Log
import com.databelay.refwatch.common.AppJsonConfiguration
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.WearSyncConstants
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Channel
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable

@AndroidEntryPoint
class WearDataListenerService : WearableListenerService() {
    private val TAG = "WearDataListenerSvc"

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var gameStorage: GameStorageWear // Injected by Hilt

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.d(TAG, "onDataChanged received ${dataEvents.count} events.")

        dataEvents.forEach { event ->
            Log.d(TAG, "Event: type=${event.type}, path=${event.dataItem.uri.path}")
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                if (dataItem.uri.path == WearSyncConstants.GAMES_LIST_PATH) {
                    Log.i(TAG, "Games list DataItem changed from phone.")
                    try {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        val gamesJsonString = dataMap.getString(WearSyncConstants.GAME_SETTINGS_KEY) // Ensure this key is correct
                        if (gamesJsonString != null) {
                            Log.d(TAG, "Received games JSON (length: ${gamesJsonString.length})")
                            // Before parsing, set status to FETCHING (or it's implicit that phone sent data)
                            // gameStorage.updateDataFetchStatus(DataFetchStatus.FETCHING) // Optional
                            val gameList = AppJsonConfiguration.decodeFromString<List<Game>>(gamesJsonString)
                            serviceScope.launch {
                                gameStorage.saveGamesListFromPhone(gameList) // This sets SUCCESS or NO_DATA_AVAILABLE
                            }
                        } else {
                            Log.w(TAG, "Games JSON string is null in DataItem. Phone sent empty data.")
                            serviceScope.launch {
                                gameStorage.saveGamesListFromPhone(emptyList()) // Results in NO_DATA_AVAILABLE
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing game list from DataItem", e)
                        serviceScope.launch {
                            gameStorage.updateDataFetchStatus(DataFetchStatus.ERROR_PARSING)
                        }
                    }
                }
            } else if (event.type == DataEvent.TYPE_DELETED) {
                if (event.dataItem.uri.path == WearSyncConstants.GAMES_LIST_PATH) {
                    Log.i(TAG, "Games list DataItem deleted by phone.")
                    serviceScope.launch {
                        gameStorage.clearGamesListFromPhone() // This sets NO_DATA_AVAILABLE
                    }
                }
            }
        }
        dataEvents.release()
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        super.onCapabilityChanged(capabilityInfo)
        Log.d(TAG, "onCapabilityChanged received: ${capabilityInfo.name}, Nodes: ${capabilityInfo.nodes.size}")

        // Check if this capability change is for the phone app we care about
        if (capabilityInfo.name == WearSyncConstants.PHONE_APP_CAPABILITY) {
            // Check if any of the nodes advertising this capability are "nearby" (connected)
            val isPhoneConnected = capabilityInfo.nodes.any { it.isNearby }

            Log.i(TAG, "Phone app capability changed. Connected: $isPhoneConnected")

            // Update the connectivity status in GameStorageWear
            // This operation should be quick. If GameStorageWear does heavy work, consider another scope.
            // For just updating a StateFlow, serviceScope (Main or IO) is fine.
            // gameStorage.updatePhoneConnectivityStatus(isPhoneConnected) // Call the method in GameStorageWear

            // More robustly, launch a coroutine for this, especially if GameStorageWear might do I/O
            serviceScope.launch { // Or a specific IO scope if GameStorageWear's method is suspend and does IO
                gameStorage.updatePhoneConnectivityStatus(isPhoneConnected)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created.")
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "Service destroyed.")
    }
}
