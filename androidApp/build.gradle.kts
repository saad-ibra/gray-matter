plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.graymatter.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.graymatter.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":shared"))
    // compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Material icons extended
    implementation("androidx.compose.material:material-icons-extended")

    // work manager
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // window dependency to get the size of the screen without insets
    implementation("androidx.window:window:1.2.0")

    // splashscreen api
    implementation("androidx.core:core-splashscreen:1.0.1")

    // kotlinx-datetime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    
    // Material components for XML themes
    implementation("com.google.android.material:material:1.11.0")
    
    // Image loading with Coil
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // PDF text extraction and advanced features
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Markdown rendering
    implementation("com.github.jeziellago:compose-markdown:0.5.0")
}