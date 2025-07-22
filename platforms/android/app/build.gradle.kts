plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

configurations.all {
    exclude(group = "com.google.guava", module = "listenablefuture")
}

android {
    namespace = "com.buccancs.gsrcapture"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.buccancs.gsrcapture"
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
                "proguard-rules.pro",
            )
        }
    }

    // Set Java version compatibility for AGP 8.x
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    // Test configuration
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }

    // Packaging options to avoid conflicts
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

// This block tells Gradle to find and use a JDK compatible with Java 17
kotlin {
    jvmToolchain(17)
}

// Create a task alias to resolve the ambiguous 'compileJava' task
tasks.register("compileJava") {
    group = "build"
    description = "Compile Java sources (delegates to debug variant)"
    dependsOn("compileDebugJavaWithJavac")
}

// Create a task alias to resolve the missing 'testClasses' task
tasks.register("testClasses") {
    group = "build"
    description = "Compile all test classes (unit tests and instrumented tests)"
    dependsOn(
        "compileDebugUnitTestJavaWithJavac",
        "compileDebugUnitTestKotlin",
        "compileDebugAndroidTestJavaWithJavac", 
        "compileDebugAndroidTestKotlin"
    )
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // AndroidX UI Components
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.exifinterface)

    // JetBrains annotations (required by Kotlin compiler)
    // Changed from compileOnly to implementation to make annotations available to the compiler toolchain
    // during build process, preventing NoClassDefFoundError during code generation
    implementation(libs.jetbrains.annotations)

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

    // Apache Commons Math (required by Shimmer library)
    implementation(libs.commons.math3)
    implementation(libs.commons.math)
    implementation(libs.vecmath) // For Shimmer library compatibility
    implementation(libs.commons.lang3) // For ArrayUtils and other utilities
    implementation(libs.guava) {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.android)

    // Local libraries from libs directory - selective inclusion to avoid duplicates
    implementation(files("libs/shimmerandroidinstrumentdriver-3.2.2_beta.aar"))
    implementation(files("libs/shimmerdriver-0.11.3_beta.jar"))
    implementation(files("libs/shimmerbluetoothmanager-0.11.3_beta.jar"))
    implementation(files("libs/shimmerdriverpc-0.11.3_beta.jar"))
    implementation(files("libs/topdon_sdk_1.5.aar"))

    // ---------- Unit-test dependencies ----------
    // JUnit 4 for unit testing
    testImplementation(libs.junit)

    // JetBrains annotations for test compilation
    testImplementation(libs.jetbrains.annotations)

    // Mockito for mocking
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockito.kotlin)

    // Truth for better assertions
    testImplementation(libs.truth) {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // Robolectric for Android unit testing
    testImplementation(libs.robolectric)

    // Coroutines testing
    testImplementation(libs.kotlinx.coroutines.test)

    // AndroidX Test Core for unit tests
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)

    // AndroidX Arch Core testing
    testImplementation(libs.androidx.arch.core.testing)

    // ---------- Instrumented-test dependencies ----------
    // AndroidX Test
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // JetBrains annotations for instrumented test compilation
    androidTestImplementation(libs.jetbrains.annotations)

    // Truth for instrumented tests
    androidTestImplementation(libs.truth) {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // AndroidX Test Rules and Runner
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)

    // Coroutines testing for instrumented tests
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
