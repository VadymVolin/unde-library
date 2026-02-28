import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "io.github.vadymvolin"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        lint.targetSdk = 36
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    api(libs.ktor.engine.client)
    api(libs.ktor.network)
    api(libs.ktor.json.serialization)
    api(libs.kotlinx.json.serialization)

    testImplementation(libs.junit)
    testImplementation(libs.junit.androidx)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

mavenPublishing {
  coordinates("io.github.vadymvolin", "unde-android-library", "0.0.3-beta")

  pom {
    name.set("Unde Android Library")
    description.set("Unde Android Library")
    inceptionYear.set("2026")
    url.set("https://github.com/VadymVolin/unde-library/")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("VadymVolin")
        name.set("Vadym Volin")
        url.set("https://github.com/VadymVolin/")
      }
    }
    scm {
      url.set("https://github.com/VadymVolin/unde-library/")
      connection.set("scm:git:git://github.com/VadymVolin/unde-library.git")
      developerConnection.set("scm:git:ssh://git@github.com/VadymVolin/unde-library.git")
    }
  }

  publishToMavenCentral()
  signAllPublications()
}
