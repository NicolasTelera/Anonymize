object Versions {

    // Build Config
    const val minSDK = 23
    const val compileSDK = 27
    const val targetSDK = 27

    // App version
    const val appVersionCode = 1
    const val appVersionName = "1.0"

    // Plugins
    const val androidGradlePlugin = "3.3.0-alpha08"

    // Google
    const val services = "4.0.1"

    // Kotlin
    const val kotlin = "1.2.61"

    // Support Lib
    const val support = "27.1.1"
    const val constraintLayout = "1.1.2"

    // Firebase
    const val firebase = "16.0.1"
    const val vision = "17.0.0"

    // Testing
    const val junit = "4.12"
    const val testRunner = "1.0.2"
    const val espresso = "3.0.2"
}

object Deps {

    // Plugins
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"

    // Google
    const val googleServices = "com.google.gms:google-services:${Versions.services}"

    // Kotlin
    const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val kotlinGradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"

    // Support Library
    const val appCompat = "com.android.support:appcompat-v7:${Versions.support}"
    const val constraintLayout = "com.android.support.constraint:constraint-layout:${Versions.constraintLayout}"

    // Firebase
    const val firebaseCore = "com.google.firebase:firebase-core:${Versions.firebase}"
    const val firebaseVision = "com.google.firebase:firebase-ml-vision:${Versions.vision}"

    // Testing
    const val junit = "junit:junit:${Versions.junit}"
    const val testRunner = "com.android.support.test:runner:${Versions.testRunner}"
    const val espressoCore = "com.android.support.test.espresso:espresso-core:${Versions.espresso}"
}
