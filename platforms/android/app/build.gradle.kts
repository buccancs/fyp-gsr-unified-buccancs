plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.buccancs.gsrcapture"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.buccancs.gsrcapture"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    // Set Java version compatibility for AGP 8.x
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.google.android.material:material:1.11.0")

    // AndroidX Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-extensions:1.3.4")

    // Third-Party Libraries
    implementation("no.nordicsemi.android:ble:2.7.5")
    implementation("com.github.mik3y:usb-serial-for-android:3.7.3")

    // Network communication
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines for asynchronous operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Apache Commons Math (required by Shimmer library)
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-math:2.2")
    implementation("java3d:vecmath:1.3.1") // For Shimmer library compatibility
    implementation("org.apache.commons:commons-lang3:3.12.0") // For ArrayUtils and other utilities
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("org.slf4j:slf4j-android:1.7.32")

    // Local libraries from libs directory - selective inclusion to avoid duplicates
    implementation(files("libs/shimmerandroidinstrumentdriver-3.2.2_beta.aar"))
    implementation(files("libs/shimmerdriver-0.11.3_beta.jar"))
    implementation(files("libs/shimmerbluetoothmanager-0.11.3_beta.jar"))
    implementation(files("libs/shimmerdriverpc-0.11.3_beta.jar"))
    implementation(files("libs/topdon_sdk_1.5.aar"))

    // ---------- Unit-test dependencies ----------
    testImplementation("junit:junit:4.13.2")

    // Mockito
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    // USB Serial for testing
    testImplementation("com.github.mik3y:usb-serial-for-android:3.7.3")

    // Robolectric
    testImplementation("org.robolectric:robolectric:4.11.1")

    // ---------- Instrumented-test dependencies ----------
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
