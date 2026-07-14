import app.cash.sqldelight.gradle.VerifyMigrationTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinPluginCompose)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.designSystem)
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            api(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.uuid)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.play.billing)
            implementation(libs.sqldelight.android)
        }
        androidUnitTest.dependencies {
            implementation(libs.sqldelight.sqlite)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
    }
}

android {
    namespace = "com.jjswigut.oopsallprs"
    compileSdk = 35
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

sqldelight {
    databases {
        create("WorkoutDatabase") {
            packageName.set("com.jjswigut.oopsallprs.db")
            schemaOutputDirectory.set(file("src/androidUnitTest/resources/schema-10"))
            // The legacy 1.sqm is not a replayable baseline. Generation must not derive from it;
            // the task override below verifies only committed schema-10 databases through 10.sqm.
            verifyMigrations.set(false)
        }
    }
}

tasks.withType<VerifyMigrationTask>().configureEach {
    verifyMigrations.set(true)
    verifyDefinitions.set(true)
}
