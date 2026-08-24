// Top-level build file. Plugins are declared here (applied in module build files).
// Note: AGP 9's built-in Kotlin compiles Kotlin sources, so there is no
// org.jetbrains.kotlin.android plugin. Only the Kotlin compiler plugins remain.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
