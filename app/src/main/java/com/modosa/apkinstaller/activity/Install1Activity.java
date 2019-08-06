package com.modosa.apkinstaller.activity;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.catchingnow.icebox.sdk_client.IceBox;
import com.modosa.apkinstaller.R;

import java.io.File;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Install1Activity extends Activity {
    private Uri uri = null;
    private String apkSourcePath;
    private Disposable mSubscribe;
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
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
        Log.e("---apkSourcePath--", apkSourcePath + "");
        startInstall(apkSourcePath, geturi);
    }

    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(Install1Activity.this, text, Toast.LENGTH_LONG).show());
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

    private void installApp(Uri uri) {
        showToast(getString(R.string.install_start));
        disposeSafety();
        mSubscribe = Single.fromCallable(() -> IceBox.installPackage(this, uri))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(success -> Toast.makeText(this, success ? getString(R.string.success_install) : getString(R.string.failed_install), Toast.LENGTH_SHORT).show(), Throwable::printStackTrace);
    }

    private void disposeSafety() {
        if (mSubscribe != null && !mSubscribe.isDisposed()){
            mSubscribe.dispose();
        }
        mSubscribe = null;
    }

    private class InstallApkTask extends Thread {
        @Override
        public void run() {
            super.run();
            new Thread(() -> {
                handler.post(() -> installApp(uri));
                finish();
            }).start();
        }
    }


}
