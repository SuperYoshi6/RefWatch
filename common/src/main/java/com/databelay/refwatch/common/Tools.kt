package com.databelay.refwatch.common

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

object LegalLinks { // Using an object to group them
    const val PRIVACY_POLICY_URL = "https://doc-hosting.flycricket.io/refwatch-privacy-policy/3571da7e-481d-4199-adfb-921382bad8be/privacy"
    const val TERMS_OF_USE_URL = "https://doc-hosting.flycricket.io/refwatch-terms-of-use/34d1063e-7d93-40d5-8016-5ede5ab4c1c1/terms"
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

