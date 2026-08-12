package com.databelay.refwatch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.databelay.refwatch.common.theme.RefWatchMobileTheme
import com.databelay.refwatch.navigation.RefWatchNavHost
import dagger.hilt.android.AndroidEntryPoint

// No need for androidx.hilt.navigation.compose.hiltViewModel here
@AndroidEntryPoint // Ensures Hilt can inject into this Activity if needed (though usually not for ViewModels directly here)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw under the system bars so the pitch-gradient bleeds edge-to-edge
        // (status + nav bar are then re-tinted transparent by the theme).
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RefWatchMobileTheme {
                RefWatchNavHost()
            }
        }
    }
}
