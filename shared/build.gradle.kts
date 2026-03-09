plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.0"
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()

    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // sql-delight runtime
                implementation("app.cash.sqldelight:runtime:2.0.0")
                // flows support for sql-delight
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.0")
                // date-time
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
            }
        }

        val androidMain by getting {
            dependencies {
                // sql-delight driver - Android
                implementation("app.cash.sqldelight:android-driver:2.0.0")
            }
        }

        val iosMain by getting {
            dependencies {
                // sql-delight driver - ios
                implementation("app.cash.sqldelight:native-driver:2.0.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                // coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                // junit
                implementation("junit:junit:4.13.2")
                implementation("androidx.test:core:1.5.0")
                // instrumentation test runner
                implementation("androidx.test:runner:1.5.2")
            }
        }
    }
}

android {
    namespace = "com.example.notes"
    compileSdk = 33
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 24
    }
}

sqldelight {
    databases {
        create("NotesDatabase") {
            packageName.set("com.example.notes.database")
            srcDirs("src/commonMain/sqldelight-notes")
        }
        create("GrayMatterDatabase") {
            packageName.set("com.example.graymatter.database")
            srcDirs("src/commonMain/sqldelight-graymatter")
        }
    }
}