package com.unde.library.internal.utils

import android.os.Build
import java.util.Locale

/**
 * Utility class for detecting device environment characteristics.
 *
 * Primarily used to determine if the application is running on an emulator
 * to adjust the server host IP address accordingly (e.g., 10.0.2.2 vs local IP).
 */
internal object DeviceManager {

    /**
     * Checks if the current device is an emulator.
     *
     * Examines various system properties (Model, Manufacturer, Hardware, etc.) to detect emulator environments.
     *
     * @return `true` if running on an emulator, `false` otherwise.
     */
    internal fun isEmulator(): Boolean = (Build.MANUFACTURER.contains("Genymotion")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.lowercase(Locale.getDefault()).contains("droid4x")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.HARDWARE == "goldfish"
                || Build.HARDWARE == "vbox86"
                || Build.HARDWARE.lowercase(Locale.getDefault()).contains("nox")
                || Build.FINGERPRINT.contains("generic")
                || Build.PRODUCT == "sdk"
                || Build.PRODUCT == "google_sdk"
                || Build.PRODUCT == "sdk_x86"
                || Build.PRODUCT == "vbox86p"
                || Build.PRODUCT.lowercase(Locale.getDefault()).contains("nox")
                || Build.BOARD.lowercase(Locale.getDefault()).contains("nox")
                || (Build.BRAND.contains("generic") && Build.DEVICE.contains("generic")))
}