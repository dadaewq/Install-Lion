package com.modosa.installdog;


import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.catchingnow.delegatedscopeclient.DSMClient;

import java.io.File;

public class InstallActivity extends Activity {
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
            }

//            String apkSourcePath = ContentUriUtils.getPath(this, geturi);
            Log.e("---apkSourcePath--", apkSourcePath + "");

//            new Thread(() -> startInstall(apkSourcePath, geturi)).start();
            startInstall(apkSourcePath, geturi);

        } else {
            showToast(getString(R.string.failed_read));
        }
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

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    private void installApp(Uri DSMuri) {
        try {
            DSMClient.installApp(this, DSMuri, null);
            showToast(getString(R.string.start_install));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(getApplication(), text, Toast.LENGTH_LONG).show());
    }

    private class InstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            Log.e("state", Thread.currentThread() + "");
            new Thread(() -> {
                Log.e("Taskuri", uri + "");
                handler.post(() -> installApp(uri));
                finish();
            }).start();
        }
    }

}
