import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("compose-common")
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)

            implementation(libs.compose.components.resources)
            implementation(libs.compose.navigation)
            implementation(libs.jgit)
            implementation(libs.pseudoterminal)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.evandhardspace.loon.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.evandhardspace.loon"
            packageVersion = "1.0.0"
        }
    }
}
