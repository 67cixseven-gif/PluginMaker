plugins {
    kotlin("jvm") version "1.9.0"
    id("com.android.library") version "8.0.0"
}

android {
    namespace = "com.aliucord.plugins.pluginmaker"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
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
    // Aliucord core
    implementation("com.aliucord:aliucord-api:")
    
    // Kotlin stdlib
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    
    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    
    // Android
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}
