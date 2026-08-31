import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// local.properties is not auto-exposed as project properties; parse it manually.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val geminiApiKey: String = localProps.getProperty("GEMINI_API_KEY", "") ?: ""
// Model IDs for the Live API change frequently — verify current supported models at
// https://ai.google.dev/gemini-api/docs/live-api and override via GEMINI_MODEL_ID in local.properties.
val geminiModelId: String = localProps.getProperty("GEMINI_MODEL_ID") ?: "gemini-3.1-flash-live-preview"
// Deployment of server/ (Vercel) that mints short-lived Live tokens. Empty = local-key dev mode.
val vercelBaseUrl: String = localProps.getProperty("VERCEL_BASE_URL", "") ?: ""

// Release signing lives in local.properties (gitignored). Absent keys mean
// the local release build stays unsigned — CI never builds release.
val keystoreFile: String? = localProps.getProperty("ALPACA_KEYSTORE_FILE")
val keystorePassword: String? = localProps.getProperty("ALPACA_KEYSTORE_PASSWORD")
val keystoreAlias: String? = localProps.getProperty("ALPACA_KEY_ALIAS")
val keystoreKeyPassword: String? = localProps.getProperty("ALPACA_KEY_PASSWORD")

android {
    namespace = "com.alpaca.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alpaca.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 7
        versionName = "0.7.0"
    }

    buildTypes {
        debug {
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
            buildConfigField("String", "GEMINI_MODEL_ID", "\"$geminiModelId\"")
            buildConfigField("String", "VERCEL_BASE_URL", "\"$vercelBaseUrl\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
            buildConfigField("String", "GEMINI_MODEL_ID", "\"$geminiModelId\"")
            buildConfigField("String", "VERCEL_BASE_URL", "\"$vercelBaseUrl\"")
            if (keystoreFile != null && keystorePassword != null && keystoreAlias != null) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = rootProject.file(keystoreFile)
                    storePassword = keystorePassword
                    keyAlias = keystoreAlias
                    keyPassword = keystoreKeyPassword ?: keystorePassword
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.billing.ktx)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
}
