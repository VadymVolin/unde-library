import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.unde.library"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        lint.targetSdk = 36
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        aarMetadata {
            minCompileSdk = 21
        }
    }

    buildTypes {
        debug {
            isDefault = true
        }

        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
}

dependencies {
    // This dependency is exported to consumers, that is to say found on their compile classpath.
    // api(libs.commons.math3) // EXAMPLE
    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    // implementation(libs.guava) // EXAMPLE

    implementation(libs.androidx.core.ktx)
    api(libs.ktor.client)
    api(libs.ktor.engine)
    api(libs.ktor.websockets)
    api(libs.ktor.json.serialization)

    // Use the Kotlin JUnit 5 integration.
    testImplementation(libs.kotlin.test.junit5)
    // Use the JUnit 5 integration.
    testImplementation(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}