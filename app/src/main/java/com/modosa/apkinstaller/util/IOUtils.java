package com.modosa.apkinstaller.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class IOUtils {
    private static final String TAG = "IOUtils";

    public static void copyStream(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[1024 * 1024];
        int len;
        while ((len = from.read(buf)) > 0) {
            to.write(buf, 0, len);
        }
    }


    public static String toString(InputStream inputStream) throws IOException {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        int charsRead;
        while ((charsRead = in.read(buffer, 0, buffer.length)) > 0) {
            out.append(buffer, 0, charsRead);
        }
        return out.toString();
    }


    public static Thread writeStreamToStringBuilder(StringBuilder builder, InputStream inputStream) {
        Thread t = new Thread(() -> {
            try {
                char[] buf = new char[1024];
                int len;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                while ((len = reader.read(buf)) > 0) {
                    builder.append(buf, 0, len);
                }

                reader.close();
            } catch (Exception e) {
                Log.wtf(TAG, e);
            }
        });
        t.start();
        return t;
    }


}
