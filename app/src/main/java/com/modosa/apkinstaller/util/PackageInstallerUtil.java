package com.modosa.apkinstaller.util;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.modosa.apkinstaller.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author heruoxin @ CatchingNow Inc.
 * @since 2018/11/6
 */
public class PackageInstallerUtil {

    public static String installPackage(Context context, Uri uri, @Nullable String packageName) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {

            final AtomicBoolean o = new AtomicBoolean();
            final StringBuilder stringBuilder = new StringBuilder();
            final String name = context.getPackageName() + "_install_" + System.currentTimeMillis();
            Context app = context.getApplicationContext();
            app.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    app.unregisterReceiver(this);
                    int statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                    if (statusCode != PackageInstaller.STATUS_SUCCESS) {
                        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

                        if (TextUtils.isEmpty(message)) {
                            stringBuilder.append(context.getString(R.string.unknown));
                        } else {
                            stringBuilder.append(message);
                        }

                    }

                    o.set(PackageInstaller.STATUS_SUCCESS == statusCode);
                    synchronized (o) {
                        o.notify();
                    }
                }
            }, new IntentFilter(name));

            PackageInstaller packageInstaller = app.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                    PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            if (!TextUtils.isEmpty(packageName)) {
                params.setAppPackageName(packageName);
            }
            // set params
            int sessionId = packageInstaller.createSession(params);
            PackageInstaller.Session session = packageInstaller.openSession(sessionId);
            OutputStream out = session.openWrite(name, 0, -1);
            byte[] buffer = new byte[65536];
            int c;
            while ((c = Objects.requireNonNull(in).read(buffer)) != -1) {
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            out.close();
            session.commit(createIntentSender(app, sessionId, name));

            synchronized (o) {
                try {
                    o.wait();
                    if (o.get()) {
                        return null;
                    } else {
                        return stringBuilder.toString();
                    }
                } catch (InterruptedException e) {
                    return stringBuilder.append("\n").toString();
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    public static String uninstallPackage(Context context, String packageName) {
        final AtomicBoolean o = new AtomicBoolean();
        final StringBuilder stringBuilder = new StringBuilder();
        final String name = context.getPackageName() + "_uninstall_" + System.currentTimeMillis();
        Context app = context.getApplicationContext();
        app.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                app.unregisterReceiver(this);
                int statusCode = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                if (statusCode != PackageInstaller.STATUS_SUCCESS) {
                    String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

                    if (TextUtils.isEmpty(message)) {
                        stringBuilder.append(context.getString(R.string.unknown));
                    } else {
                        stringBuilder.append(message);
                    }
                }
                o.set(PackageInstaller.STATUS_SUCCESS == statusCode);
                synchronized (o) {
                    o.notify();
                }
            }
        }, new IntentFilter(name));

        PackageInstaller mPackageInstaller = app.getPackageManager().getPackageInstaller();
        try {
            mPackageInstaller.uninstall(packageName, createIntentSender(app, name.hashCode(), name));
        } catch (Exception e) {
            return e.toString();
        }


        synchronized (o) {
            try {
                o.wait();
                if (o.get()) {
                    return null;
                } else {
                    return stringBuilder.toString();
                }
            } catch (InterruptedException e) {
                return stringBuilder.append("\n").toString();
            }
        }
    }


    private static IntentSender createIntentSender(Context context, int sessionId, String name) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(name),
                0);
        return pendingIntent.getIntentSender();
    }
}
