import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose.hot-reload")
}

configure<KotlinMultiplatformExtension> {
    jvm()

    compilerOptions {
        freeCompilerArgs.set(listOf("-Xcontext-parameters"))
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libraries.findBundle("compose-core").get())
            implementation(libraries.findBundle("androidx-lifecycle").get())
        }
        commonTest.dependencies {
            implementation(libraries.findLibrary("kotlin-test").get())
        }
        jvmMain.dependencies {
            implementation(libraries.findLibrary("kotlinx-coroutinesSwing").get())
        }
    }
}