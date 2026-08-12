package com.databelay.refwatch.data

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GameLogViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel() {
    val isDfbNetEnabled: Boolean
        get() = preferencesManager.isDfbNetEnabled
}
