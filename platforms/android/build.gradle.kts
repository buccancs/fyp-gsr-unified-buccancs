// Top-level build file where you can add configuration options common to all sub-projects/modules.

// Use aliases from libs.versions.toml for plugins
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register("clean", Delete::class) {
    description = "Clean build directory"
    delete(rootProject.layout.buildDirectory)
}
