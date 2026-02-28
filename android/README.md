# Unde Android Library

Unde is a powerful network interception and debugging library for Android. It allows developers to inspect network traffic, cache responses for offline debugging, and forward requests to a connected desktop tool for advanced analysis.

## Features

- **Network Interception**: Capture HTTP/HTTPS requests and responses.
- **Real-time Forwarding**: detailed network logs sent to a connected local desktop server (via TCP).
- **Offline Caching**: Automatically cache network responses (GZIP compressed) to the local device storage for later inspection or playback.
- **Emulator Support**: Automatically detects if running on an emulator to adjust server connection (10.0.2.2 support).
- **Architecture**: Clean separation of internal proxies and external plugin interfaces.

## Installation

Add the dependency to your module-level `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.vadymvolin:unde-android-library:0.0.3-beta")
}
```

## Usage

### 1. Initialize the Library

Initialize Unde in your `Application` class. You can optionally provide custom file paths for caching.

```kotlin
// In your Application.onCreate()
import com.unde.library.UndeLibrary

class MyApplication : Application() {
    private val undeLibrary = UndeLibrary()

    override fun onCreate() {
        super.onCreate()
        // Initialize with default settings
        undeLibrary.initialize() 
        
        // OR with custom cache directories
        // undeLibrary.initialize(networkCacheFile = File(cacheDir, "network_cache"), databaseCacheFile = ...)
    }
}
```

### 2. Add the Interceptor

Add the `UndeHttpInterceptor` to your OkHttp client to start capturing traffic.

```kotlin
import com.unde.library.external.network.interceptor.okhttp.UndeHttpInterceptor
import okhttp3.OkHttpClient

val client = OkHttpClient.Builder()
    .addInterceptor(UndeHttpInterceptor())
    .build()
```

## Architecture

The library is organized into:
- **`external`**: Public APIs for integration (Interceptors).
- **`internal`**: Core logic not meant for direct consumer use.
    - **`proxy`**: Handles socket connections (`ServerSocketProxy`) and file caching (`CacheProxy`).
    - **`plugin`**: Bridges interceptors to the proxy logic (`UndeNetworkPlugin`).

## Development Guide

### Prerequisites
- JDK 21
- Android SDK

### Build the Project
To compile the library and run tests:

```bash
./gradlew build
```

### Publishing
To publish the library to Maven Central (requires valid credentials in `local.properties` or environment variables):

```bash
./gradlew publishToMavenCentral
```
