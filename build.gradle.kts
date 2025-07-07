// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    id("com.android.application") version "8.8+" apply false
    id("org.jetbrains.kotlin.android") version "2.0+" apply false
}

tasks.register("clean", Delete::class) {
    description = "Clean build directory"
    delete(rootProject.layout.buildDirectory)
}
