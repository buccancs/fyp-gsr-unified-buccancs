// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // SonarQube for the whole build
    alias(libs.plugins.sonarqube)

    id("jacoco")
}

/**
 * SonarQube configuration for code quality analysis
 * Using SonarCloud for free analysis - can be overridden via command line or CI
 */
sonarqube {
    properties {
        // Project identification
        property("sonar.projectKey", "gsr-bucika")
        property("sonar.projectName", "GSR Bucika")
        property("sonar.projectVersion", "1.0")
        property("sonar.organization", "bucika") // Update this to your SonarCloud organization

        // SonarCloud URL (free for open source projects)
        property("sonar.host.url", "https://sonarcloud.io")

        // Source and test directories
        property("sonar.sources", "app/src/main/java,app/src/main/kotlin")
        property("sonar.tests", "app/src/test,app/src/androidTest")

        // Binary directories for analysis
        property(
            "sonar.java.binaries",
            "app/build/intermediates/javac/debug/classes,app/build/tmp/kotlin-classes/debug"
        )
        property("sonar.kotlin.binaries", "app/build/tmp/kotlin-classes/debug")

        // Android specific settings
        property("sonar.android.lint.report", "app/build/reports/lint-results-debug.xml")

        // Code coverage integration
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
        )

        // Language settings
        property("sonar.java.source", "21")
        property("sonar.kotlin.source", "2.2.0")

        // Exclusions for generated code and third-party libraries
        property(
            "sonar.exclusions",
            "**/R.java," +
                    "**/BuildConfig.java," +
                    "**/Manifest*.*," +
                    "**/*Test*.*," +
                    "**/libs/**," +
                    "**/third_party/**," +
                    "**/build/**"
        )

        // Test exclusions
        property("sonar.test.exclusions", "**/test/**,**/androidTest/**")

        // Quality gate settings
        property("sonar.qualitygate.wait", "true")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
