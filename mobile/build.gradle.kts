plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services") // If your mobile app uses Firebase directly
    id("com.google.devtools.ksp")        // Apply KSP if you use it for Room, etc.
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.databelay.refwatch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.databelay.refwatch"
        minSdk = 31
        targetSdk = 36
        versionCode = 361160000
        versionName = "1.6.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(project(":common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.play.services.auth) // Check for latest
    implementation(libs.play.services.wearable)
    implementation(libs.material)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.compose.ui)
    implementation(libs.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.compose.material) // Replace with latest version
    implementation(libs.kotlinx.coroutines.android) // You likely have this or core
    implementation(libs.kotlinx.coroutines.play.services) // Or the latest version
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.gson) // Or latest version
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)


    debugImplementation(libs.mockito.core)
    testImplementation(libs.junit)
    testImplementation(libs.google.truth) // Or a newer version
    // For Android Instrumented tests (like yours in androidTest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.google.truth) // Or
    ksp(libs.hilt.compiler) // Or kapt
}