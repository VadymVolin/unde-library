package com.unde.library.internal.utils

import android.os.Build
import java.util.Locale

internal object DeviceManager {

    internal fun isEmulator(): Boolean {
        return (Build.MANUFACTURER.contains("Genymotion")
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
}