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

val verifyLauncherIconIdentity by tasks.registering {
    group = "verification"
    description = "Verifies production and debug launcher icon resources stay distinct and correctly wired."

    val mainResDir = layout.projectDirectory.dir("src/main/res")
    val debugResDir = layout.projectDirectory.dir("src/debug/res")
    inputs.dir(mainResDir)
    inputs.dir(debugResDir)

    doLast {
        val productionAdaptiveIcon = mainResDir.file("mipmap-anydpi-v26/ic_launcher.xml").asFile.readText()
        val productionRoundIcon = mainResDir.file("mipmap-anydpi-v26/ic_launcher_round.xml").asFile.readText()
        val debugAdaptiveIcon = debugResDir.file("mipmap-anydpi-v26/ic_launcher.xml").asFile.readText()
        val debugRoundIcon = debugResDir.file("mipmap-anydpi-v26/ic_launcher_round.xml").asFile.readText()

        require("@drawable/ic_launcher_background" in productionAdaptiveIcon)
        require("@mipmap/ic_launcher_foreground" in productionAdaptiveIcon)
        require("@drawable/ic_launcher_background" in productionRoundIcon)
        require("@mipmap/ic_launcher_foreground" in productionRoundIcon)

        listOf(debugAdaptiveIcon, debugRoundIcon).forEach { iconXml ->
            require("@drawable/ic_launcher_debug_background" in iconXml)
            require("@mipmap/ic_launcher_debug_foreground" in iconXml)
            require("@drawable/ic_launcher_debug_monochrome" in iconXml)
            require("@mipmap/ic_launcher_foreground" !in iconXml)
            require("@drawable/ic_launcher_background" !in iconXml)
        }

        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            val densityDir = debugResDir.dir("mipmap-$density")
            require(densityDir.file("ic_launcher.png").asFile.isFile)
            require(densityDir.file("ic_launcher_round.png").asFile.isFile)
            require(densityDir.file("ic_launcher_debug_foreground.png").asFile.isFile)
        }
    }
}

tasks.named("check") {
    dependsOn(verifyLauncherIconIdentity)
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
