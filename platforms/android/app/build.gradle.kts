plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Use aliases from libs.versions.toml
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // AndroidX Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // Third-Party Libraries
    implementation(libs.nordic.ble)
    implementation(libs.usb.serial.android)

    // Network communication
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)

    // Coroutines for asynchronous operations
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Local libraries from libs directory - selective inclusion to avoid duplicates
    implementation(files("libs/shimmerandroidinstrumentdriver-3.2.2_beta.aar"))
    implementation(files("libs/shimmerdriver-0.11.3_beta.jar"))
    implementation(files("libs/shimmerbluetoothmanager-0.11.3_beta.jar"))
    implementation(files("libs/shimmerdriverpc-0.11.3_beta.jar"))
    implementation(files("libs/topdon_sdk_1.5.aar"))

    // ---------- Unit-test dependencies ----------
    testImplementation(libs.junit)

    // Mockito (using consistent versions from libs.versions.toml)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)

    // USB Serial for testing
    testImplementation("com.github.mik3y:usb-serial-for-android:${libs.versions.usbSerial.get()}") // Add USB serial dependency for tests

    // Robolectric (latest published stable)
    testImplementation("org.robolectric:robolectric:4.11.1")

    // ---------- Instrumented-test dependencies ----------
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
