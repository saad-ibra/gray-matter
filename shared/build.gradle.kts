import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.0"
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    targetHierarchy.default()
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
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
                // koin DI
                implementation("io.insert-koin:koin-core:3.5.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // sql-delight driver - Android
                implementation("app.cash.sqldelight:android-driver:2.0.0")
                // SQLCipher for encrypted database (AES-256)
                implementation("net.zetetic:android-database-sqlcipher:4.5.4")
                implementation("androidx.sqlite:sqlite:2.4.0")
            }
        }
    }
}

android {
    namespace = "com.example.graymatter"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

sqldelight {
    databases {
        create("GrayMatterDatabase") {
            packageName.set("com.example.graymatter.database")
            srcDirs("src/commonMain/sqldelight-graymatter")
        }
        create("NotesDatabase") {
            packageName.set("com.example.notes.database")
            srcDirs("sqldelight-notes")
        }
    }
}