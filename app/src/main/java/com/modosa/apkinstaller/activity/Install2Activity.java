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


public class Install2Activity extends Activity {
    private Uri uri = null;
    private String apkSourcePath;
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri geturi = getIntent().getData();
        if (geturi != null) {
            Log.e("---getIntent--", getIntent().getData() + "");
            if ("file".equalsIgnoreCase(geturi.getScheme())) {
                apkSourcePath = geturi.getPath();
            } else if ("content".equalsIgnoreCase(geturi.getScheme())) {
                apkSourcePath = null;
            } else {
                showToast("无法解析" + geturi);
                finish();
            }
            Log.e("---apkSourcePath--", apkSourcePath + "");
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
        startInstall(apkSourcePath, geturi);
    }

    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(Install2Activity.this, text, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    private void startInstall(String apkSourcePath, Uri geturi) {
        if (apkSourcePath != null) {
            String authority = getPackageName() + ".FILE_PROVIDER";
            uri = FileProvider.getUriForFile(getApplicationContext(), authority, new File(apkSourcePath));
        } else {
            uri = geturi;
        }
        handler = new Handler(Looper.getMainLooper());
        new InstallApkTask().start();
    }

    private void installApp(Uri DSMuri) {
        try {
            DSMClient.installApp(this, DSMuri, null);
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
                Log.e("Taskuri", uri + "");
                handler.post(() -> installApp(uri));
                finish();
            }).start();
        }
    }

}
