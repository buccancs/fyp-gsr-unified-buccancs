plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf") version "0.9.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20"
    id("jacoco")
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", "gsr-bucika")
        property("sonar.organization", "bucika")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.gradle.skipCompile", "true")

        // Source and test directories
        property("sonar.sources", "src/main/java,src/main/kotlin")
        property(
            "sonar.tests",
            "src/test/java,src/test/kotlin,src/androidTest/java,src/androidTest/kotlin"
        )

        // Binary directories for analysis
        property(
            "sonar.java.binaries",
            "${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug"
        )
        property(
            "sonar.kotlin.binaries",
            "${layout.buildDirectory.get().asFile}/tmp/kotlin-classes/debug"
        )

        // Exclude generated code and files not relevant for analysis
        property(
            "sonar.exclusions", listOf(
                "**/generated/**",
                "**/build/**",
                "**/*Test*.*",
                "**/test/**",
                "**/androidTest/**",
                "**/R.class",
                "**/R\$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/databinding/**",
                "**/binding/**",
                "**/BR.*",
                "**/*JsonAdapter.class",
                "**/*\$ViewInjector*.*",
                "**/*\$ViewBinder*.*",
                "**/Lambda\$*.class",
                "**/Lambda.class",
                "**/*Lambda.class",
                "**/*Lambda*.class",
                "**/*\$InjectAdapter.class",
                "**/*\$ModuleAdapter.class",
                "**/*\$ViewInjector.class",
                "**/third_party/**",
                "**/libs/**"
            ).joinToString(",")
        )

        // Coverage and lint reports
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${layout.buildDirectory.get().asFile}/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )
        property(
            "sonar.android.lint.reportPaths",
            "${layout.buildDirectory.get().asFile}/reports/lint-results-debug.xml"
        )

        // Language-specific settings
        property("sonar.java.source", "8")
        property("sonar.kotlin.source", "1.9")

        // Additional quality settings
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.verbose", "false")
    }
}

android {
    namespace = "com.gsrmultimodal.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gsrmultimodal.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            // Enable code coverage for debug builds
            buildConfigField("boolean", "DEBUG_MODE", "true")
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release")

            buildConfigField("boolean", "DEBUG_MODE", "false")
            buildConfigField("String", "BUILD_TYPE", "\"release\"")
        }

        create("staging") {
            initWith(getByName("debug"))
            isDebuggable = true
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            buildConfigField("boolean", "DEBUG_MODE", "true")
            buildConfigField("String", "BUILD_TYPE", "\"staging\"")
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    protobuf {
        protoc {
            artifact = "com.google.protobuf:protoc:3.24.4"
        }
        generateProtoTasks {
            all().forEach { task ->
                task.builtins {
                    create("java") {
                        option("lite")
                    }
                    create("kotlin") {
                        option("lite")
                    }
                }
            }
        }
    }
}





dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)

    // CameraX dependencies
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // Coroutines for background processing
    implementation(libs.kotlinx.coroutines.android)

    // Shimmer SDK for GSR sensor - Local files from third_party/shimmer-sdk
    implementation(files("../../third_party/shimmer-sdk/shimmerandroidinstrumentdriver-3.2.2_beta.aar"))
    implementation(files("../../third_party/shimmer-sdk/shimmerbluetoothmanager-0.11.3_beta.jar"))
    implementation(files("../../third_party/shimmer-sdk/shimmerdriver-0.11.3_beta.jar"))

    // Topdon SDK (placeholder - actual implementation files should be placed in libs/)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    // Networking and data streaming
    implementation(libs.okhttp)
    implementation(libs.okio)

    // JSON processing
    implementation(libs.kotlinx.serialization.json)

    // Protocol Buffers
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.protobuf.java.util)

    // Lab Streaming Layer (LSL) for Android - Temporarily commented out due to dependency issues
    // implementation(libs.lsl)

    // MediaPipe for hand detection and pose estimation - Temporarily commented out due to dependency issues
    // implementation(libs.mediapipe.java)
    // implementation(libs.mediapipe.android)

    // ML Kit for additional hand analysis capabilities
    implementation(libs.mlkit.pose.detection)
    implementation(libs.mlkit.pose.detection.accurate)

    // OpenCV for image processing - Temporarily commented out due to dependency issues
    // implementation(libs.opencv.android)

    // Topdon SDK (placeholder - actual implementation files should be placed in libs/)
    implementation(fileTree(mapOf("dir" to "../../third_party/topdon-sdk", "include" to listOf("*.jar", "*.aar"))))

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("org.mockito:mockito-android:5.7.0")
}
