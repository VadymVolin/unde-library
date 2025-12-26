import java.util.Properties
import java.io.FileInputStream

// Load secrets from secrets.properties and inject as system properties
val secretsPropertiesFile = file("secrets.properties")
if (secretsPropertiesFile.exists()) {
    val secretsProperties = Properties()
    secretsProperties.load(FileInputStream(secretsPropertiesFile))
    
    // Set each property as a system property so it's available as environment variable
    secretsProperties.forEach { key, value ->
        System.setProperty(key.toString(), value.toString())
    }
}

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}