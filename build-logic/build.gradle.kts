plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.plugin.kotlin.gradle)
    implementation(libs.plugin.compose.multiplatform)
    implementation(libs.plugin.compose.compiler)
    implementation(libs.plugin.compose.hotReload)
}
