plugins {
    alias(libs.plugins.androidApplication) apply false
    id("com.google.gms.google-services") version "4.3.15" apply false // keep firebase services aligned
}

