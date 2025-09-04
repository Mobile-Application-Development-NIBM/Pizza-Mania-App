plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlinAndroid)
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.3.0")

    // ❌ REMOVE Firestore since you won't use it anymore
    // implementation("com.google.firebase:firebase-firestore:24.10.0")

    // ✅ Keep storage only if your app uses file/image upload
    implementation("com.google.firebase:firebase-storage:20.3.0")

    // ✅ Add Realtime Database
    implementation("com.google.firebase:firebase-database:20.3.1")
}
