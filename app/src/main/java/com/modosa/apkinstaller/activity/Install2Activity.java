package com.modosa.apkinstaller.activity;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.catchingnow.delegatedscopeclient.DSMClient;
import com.modosa.apkinstaller.R;

import java.io.File;

/**
 * @author dadaewq
 */
public class Install2Activity extends Activity {
    private Uri uri = null;
    private String apkSourcePath;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri getUri = getIntent().getData();
        if (getUri != null) {
            Log.e("---getIntent--", getIntent().getData() + "");
            String content = "content";
            String file = "file";
            if (file.equalsIgnoreCase(getUri.getScheme())) {
                apkSourcePath = getUri.getPath();
            } else if (content.equalsIgnoreCase(getUri.getScheme())) {
                apkSourcePath = null;
            } else {
                showToast("无法解析" + getUri);
                finish();
            }
            Log.e("---apkSourcePath--", apkSourcePath + "");
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
        startInstall(apkSourcePath, getUri);
    }

    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(Install2Activity.this, text, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    private void startInstall(String apkSourcePath, Uri getUri) {
        if (apkSourcePath != null) {
            String authority = getPackageName() + ".FILE_PROVIDER";
            uri = FileProvider.getUriForFile(getApplicationContext(), authority, new File(apkSourcePath));
        } else {
            uri = getUri;
        }
        handler = new Handler(Looper.getMainLooper());
        new InstallApkTask().start();
    }

    private void installApp(Uri uri_DSM) {
        try {
            DSMClient.installApp(this, uri_DSM, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        showToast(getString(R.string.install_end));

    }

    private class InstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
//            Log.e("state", Thread.currentThread() + "");
            new Thread(() -> {
                handler.post(() -> installApp(uri));
                finish();
            }).start();
        }
    }

}
