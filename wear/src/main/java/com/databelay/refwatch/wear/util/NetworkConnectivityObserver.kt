package com.databelay.refwatch.wear.util // Or your chosen package

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityObserver.Status> {
        return callbackFlow {
            // Initial check
            val initialStatus = getCurrentConnectivityStatus()
            launch { send(initialStatus) }

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    launch { send(ConnectivityObserver.Status.AVAILABLE) }
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    launch { send(ConnectivityObserver.Status.LOSING) }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    // Check if there are other available networks before declaring completely lost
                    if (!isAnyNetworkAvailable()) {
                        launch { send(ConnectivityObserver.Status.LOST) }
                    } else {
                        // Still might have another network (e.g., switched from WiFi to Cellular)
                        // This case is nuanced; onAvailable for the new network should trigger.
                        // For simplicity, if one specific network is lost, we check overall status.
                        launch { send(getCurrentConnectivityStatus()) } // re-evaluate overall status
                    }
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    // This callback is for when a specific request cannot be fulfilled
                    // but doesn't mean all connectivity is lost. Check overall status.
                    if (!isAnyNetworkAvailable()) {
                        launch { send(ConnectivityObserver.Status.UNAVAILABLE) }
                    }
                }
            }

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
        }.distinctUntilChanged() // Only emit when status actually changes
    }

    private fun isAnyNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }

    private fun getCurrentConnectivityStatus(): ConnectivityObserver.Status {
        return if (isAnyNetworkAvailable()) {
            ConnectivityObserver.Status.AVAILABLE
        } else {
            ConnectivityObserver.Status.UNAVAILABLE
        }
    }
}
