import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "app.homeflix.tv.core.network"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testFixtures {
        enable = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    parallel = true
}

ktlint {
    android.set(true)
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    classpath.setFrom(tasks.named<KotlinCompile>("compileDebugKotlin").map { it.libraries })
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.okhttp.mockwebserver)
    testRuntimeOnly(libs.junit.platform.launcher)
}
