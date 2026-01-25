# Useful commands


## Run tests:

```shell
./gradlew :unde-android-library:testDebugUnitTest --tests "com.unde.library.UndeLibraryTest"
```


## Build debug version:

```shell
./gradlew clean assembleDebug
```

## Build release version:

```shell
./gradlew clean :unde-android-library:assembleRelease
```
