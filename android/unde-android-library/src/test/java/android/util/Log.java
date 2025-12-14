package android.util;

import androidx.annotation.Nullable;

/**
 * Mocked android.util.Log for test purposes
 */
public class Log {

    public static boolean isLoggable(@Nullable String var0, int var1) {
        return false;
    }

    public static int d(String tag, String msg) {
        System.out.println(tag + ": " + msg);
        return 0;
    }
    public static int i(String tag, String msg) { return d(tag, msg); }
    public static int e(String tag, String msg) { return d(tag, msg); }
}
