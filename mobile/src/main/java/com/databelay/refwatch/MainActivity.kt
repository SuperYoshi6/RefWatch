package com.databelay.refwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.databelay.refwatch.common.theme.RefWatchMobileTheme
import com.databelay.refwatch.navigation.RefWatchNavHost
import dagger.hilt.android.AndroidEntryPoint

// No need for androidx.hilt.navigation.compose.hiltViewModel here
@AndroidEntryPoint // Ensures Hilt can inject into this Activity if needed (though usually not for ViewModels directly here)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RefWatchMobileTheme {
                RefWatchNavHost()
            }
        }
    }
}
