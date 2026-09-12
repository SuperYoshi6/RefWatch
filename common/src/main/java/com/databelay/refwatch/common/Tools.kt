package com.databelay.refwatch.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.navigation.NavController

object LegalLinks {
    const val PRIVACY_POLICY_URL = "https://doc-hosting.flycricket.io/refwatch-privacy-policy/3571da7e-481d-4199-adfb-921382bad8be/privacy"
    const val PRIVACY_POLICY_EN_URL = "https://doc-hosting.flycricket.io/refwatch-privacy-policy/3571da7e-481d-4199-adfb-921382bad8be/privacy" // Replace with EN if available
    const val TERMS_OF_USE_URL = "https://doc-hosting.flycricket.io/refwatch-terms-of-use/34d1063e-7d93-40d5-8016-5ede5ab4c1c1/terms"
    const val TERMS_OF_USE_EN_URL = "https://doc-hosting.flycricket.io/refwatch-terms-of-use/34d1063e-7d93-40d5-8016-5ede5ab4c1c1/terms" // Replace with EN if available
    const val WEBSITE_URL = "https://superyoshi6.github.io/RefWatch/"
    const val WEBSITE_EN_URL = "https://superyoshi6.github.io/RefWatch/en/"
    const val WEB_MANAGER_URL = "https://superyoshi6.github.io/RefWatch/manager/"
    const val GITHUB_URL = "https://github.com/SuperYoshi6/RefWatch"
    const val GITHUB_ORIGINAL_URL = "https://github.com/githubbar/RefWatch"

    fun getLocalizedWebsiteUrl(languageCode: String): String {
        return if (languageCode == "de") WEBSITE_URL else WEBSITE_EN_URL
    }

    fun getLocalizedPrivacyUrl(languageCode: String): String {
        return if (languageCode == "de") PRIVACY_POLICY_URL else PRIVACY_POLICY_EN_URL
    }

    fun getLocalizedTermsUrl(languageCode: String): String {
        return if (languageCode == "de") TERMS_OF_USE_URL else TERMS_OF_USE_EN_URL
    }
}

// Function to get the application's version name
fun getAppVersionName(context: Context): String {
    return try {
        val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "N/A" // Return "N/A" or some default if null
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        "N/A" // Or handle the exception as appropriate
    }
}

// Function to get the application's version code
fun getAppVersionCode(context: Context): Long { // Or Int if you don't expect very large version codes
    return try {
        val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        -1L // Or handle the exception as appropriate
    }
}


@SuppressLint("RestrictedApi")
fun logBackStack(navController: NavController, contextMessage: String = "") {
    val stack = navController.currentBackStack.value
    val currentNavControllerDestination = navController.currentDestination
    val currentNavControllerRoute = currentNavControllerDestination?.route
    val currentNavControllerId = currentNavControllerDestination?.id
    val currentNavControllerClass =
        currentNavControllerDestination?.displayName

    val TAG = "LogBackStack"
    Log.d("${TAG}:stack", "---- NavController Back Stack ($contextMessage) ----")
    Log.d(
        "${TAG}:stack",
        "NavController Current Destination: Route='${currentNavControllerRoute ?: "null"}', ID='${currentNavControllerId ?: "null"}', Class='${currentNavControllerClass ?: "null"}'"
    )

    if (stack.isEmpty()) {
        Log.d("${TAG}:stack", "Back stack is empty.")
    } else {
        stack.forEachIndexed { index, navBackStackEntry ->
            val entryDestination = navBackStackEntry.destination
            val route = entryDestination.route
            val arguments = navBackStackEntry.arguments?.toString() ?: "null"
            val destDisplayName = entryDestination.displayName

            Log.d(
                "${TAG}:stack",
                "$index: Route='${route ?: "null"}', Args=[$arguments], ID='${navBackStackEntry.id}', NavDestId='${entryDestination.id}', NavDestClass='${destDisplayName}'"
            )
        }
    }
    Log.d("${TAG}:stack", "------------------------------------------")
}

/**
 * Calculates the match minute for a game event, handling added time (e.g., 45+2).
 */
fun GameEvent.getMatchMinute(game: Game): String {
    return getMatchMinute(game.halfDurationMinutes, game.extraTimeHalfDurationMinutes)
}

fun GameEvent.getMatchMinute(halfMin: Int, etMin: Int = 15): String {
    val phase = this.phase ?: return ""
    val elapsedMillis = this.gameTimeMillis.toLong()

    val (baseStart, baseEnd, regMillis) = when (phase) {
        GamePhase.FIRST_HALF -> Triple(0, halfMin, halfMin * 60000L)
        GamePhase.SECOND_HALF -> Triple(halfMin, halfMin * 2, halfMin * 60000L)
        GamePhase.EXTRA_TIME_FIRST_HALF -> Triple(halfMin * 2, halfMin * 2 + etMin, etMin * 60000L)
        GamePhase.EXTRA_TIME_SECOND_HALF -> Triple(halfMin * 2 + etMin, halfMin * 2 + etMin * 2, etMin * 60000L)
        else -> return ""
    }

    return if (elapsedMillis > regMillis) {
        val addedMinutes = ((elapsedMillis - regMillis + 59999) / 60000).toInt()
        "$baseEnd+$addedMinutes"
    } else {
        val currentMin = ((elapsedMillis + 59999) / 60000).toInt()
        "${baseStart + currentMin}'"
    }
}


