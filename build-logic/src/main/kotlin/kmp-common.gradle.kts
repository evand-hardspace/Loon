import gradle.kotlin.dsl.accessors._b37ed69b5479be81513a9a46516ac880.sourceSets
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

configure<KotlinMultiplatformExtension> {
    jvm()

    compilerOptions {
        freeCompilerArgs.set(listOf("-Xcontext-parameters"))
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libraries.findLibrary("kotlinx-coroutines").get())
        }
        commonTest.dependencies {
            implementation(libraries.findLibrary("kotlin-test").get())
        }
    }
}