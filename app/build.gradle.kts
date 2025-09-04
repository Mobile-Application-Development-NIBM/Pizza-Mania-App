plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.10"
    id("com.google.gms.google-services") version "4.3.15" // match project-level
    kotlin("kapt") version "1.9.10"
}

android {
    namespace = "com.example.pizzamaniaapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pizzamaniaapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // ✅ Match Java and Kotlin versions
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.3.0")

    // ❌ Removed Firestore since not used
    // implementation("com.google.firebase:firebase-firestore:24.10.0")

    // ✅ Keep storage only if needed
    implementation("com.google.firebase:firebase-storage:20.3.0")

    // ✅ Add Realtime Database
    implementation("com.google.firebase:firebase-database:20.3.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    implementation ("com.google.android.material:material:1.9.0")

}
