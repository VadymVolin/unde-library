import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "com.unde.library"
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
    api(libs.ktor.websockets)
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
  coordinates("com.example.mylibrary", "mylibrary-runtime", "1.0.3-SNAPSHOT")

  pom {
    name.set("My Library")
    description.set("A description of what my library does.")
    inceptionYear.set("2020")
    url.set("https://github.com/username/mylibrary/")
    licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
        distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    developers {
      developer {
        id.set("username")
        name.set("User Name")
        url.set("https://github.com/username/")
      }
    }
    scm {
      url.set("https://github.com/username/mylibrary/")
      connection.set("scm:git:git://github.com/username/mylibrary.git")
      developerConnection.set("scm:git:ssh://git@github.com/username/mylibrary.git")
    }
  }
}