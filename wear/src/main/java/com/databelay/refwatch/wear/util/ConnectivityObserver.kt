package com.databelay.refwatch.wear.util // Or your chosen package

import kotlinx.coroutines.flow.Flow

interface ConnectivityObserver {
    fun observe(): Flow<Status>

    enum class Status {
        UNINITIALIZED, // Initial state before first check
        AVAILABLE,     // Network is available
        UNAVAILABLE,   // Network is unavailable
        LOSING,        // Network is losing connectivity
        LOST           // Network has been lost
    }
}