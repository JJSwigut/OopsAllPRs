plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinPluginCompose)
}

val ciVersionCode = providers.gradleProperty("android.injected.version.code")
    .orElse(providers.environmentVariable("ANDROID_VERSION_CODE"))
    .map(String::toInt)
    .getOrElse(1)
val ciVersionName = providers.gradleProperty("android.injected.version.name")
    .orElse(providers.environmentVariable("ANDROID_VERSION_NAME"))
    .getOrElse("1.0")
val releaseSigningConfigured = listOf(
    "ANDROID_UPLOAD_KEYSTORE_PATH",
    "ANDROID_UPLOAD_KEYSTORE_PASSWORD",
    "ANDROID_UPLOAD_KEY_ALIAS",
    "ANDROID_UPLOAD_KEY_PASSWORD",
).all { !System.getenv(it).isNullOrBlank() }

android {
    namespace = "com.jjswigut.oopsallprs.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jjswigut.oopsallprs.android"
        minSdk = 31
        targetSdk = 35
        versionCode = ciVersionCode
        versionName = ciVersionName
        manifestPlaceholders["appLabel"] = "Oops All PRs"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(System.getenv("ANDROID_UPLOAD_KEYSTORE_PATH"))
                storePassword = System.getenv("ANDROID_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "Oops All PRs Debug"
        }
        release {
            manifestPlaceholders["appLabel"] = "Oops All PRs"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

dependencies {
    implementation(projects.shared)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
}
