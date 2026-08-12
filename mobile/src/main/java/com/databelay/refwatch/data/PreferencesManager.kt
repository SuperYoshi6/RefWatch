package com.databelay.refwatch.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("refwatch_prefs", Context.MODE_PRIVATE)

    var isDfbNetEnabled: Boolean
        get() = sharedPreferences.getBoolean("dfbnet_enabled", true)
        set(value) = sharedPreferences.edit().putBoolean("dfbnet_enabled", value).apply()
}
