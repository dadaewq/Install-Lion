package com.modosa.apkinstaller.utils;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {

    public static String throwableToString(Throwable throwable) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);
        pw.close();

        return sw.toString();
    }

    @SuppressLint("PrivateApi")
    @Nullable
    private static String getSystemProperty() {
        try {
            return (String) Class.forName("android.os.SystemProperties")
                    .getDeclaredMethod("get", String.class)
                    .invoke(null, "ro.miui.ui.version.name");
        } catch (Exception e) {
            Log.w("SAIUtils", "Unable to use SystemProperties.get", e);
            return null;
        }
    }


    public static boolean isMiui() {
        return !TextUtils.isEmpty(getSystemProperty());
    }


}
