package com.modosa.apkinstaller.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.modosa.apkinstaller.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * @author dadaewq
 */
public abstract class AbstractInstallActivity extends Activity {
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    boolean istemp = false;
    String[] apkinfo;
    boolean show_notification;
    private Uri uri;
    private boolean needrequest;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private AlertDialog alertDialog;
    private String cachePath;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uri = getIntent().getData();

        assert uri != null;
        needrequest = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (ContentResolver.SCHEME_FILE.equals(uri.getScheme()));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        show_notification = sharedPreferences.getBoolean("show_notification", false);

        init();

    }


    private void init() {
        boolean needconfirm = sharedPreferences.getBoolean("needconfirm", true);
        String apkPath = preInstall();
        apkinfo = getApkPkgInfo(apkPath);
        cachePath = apkPath;
        if (needconfirm) {
            Context context = new ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Dialog);
            CharSequence[] items = new CharSequence[]{getString(R.string.items)};
            boolean[] checkedItems = {false};
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            if (apkinfo == null) {
                builder.setTitle(getString(R.string.dialog_install_title));
            } else {
                builder.setTitle(getString(R.string.dialog_install_title) + " " + apkinfo[1]);
            }


            builder.setMultiChoiceItems(items, checkedItems, (dialogInterface, i, b) -> {
                if (b) {
                    editor = sharedPreferences.edit();
                    editor.putBoolean("needconfirm", false);
                    editor.apply();
                } else {
                    editor = sharedPreferences.edit();
                    editor.putBoolean("needconfirm", true);
                    editor.apply();
                }
            });
            builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                cachePath = null;
                AbstractInstallActivity.this.startInstall(apkPath);
                AbstractInstallActivity.this.finish();
            });
            builder.setNegativeButton(android.R.string.no, (dialog, which) -> finish());
            builder.setCancelable(false);
            alertDialog = builder.show();

        } else {
            startInstall(apkPath);
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (needrequest) {
            confirmPermission();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (istemp && (cachePath != null)) {
            deleteSingleFile(new File(cachePath));
        }
        alertDialog.dismiss();
    }


    private String preInstall() {
        String apkPath = null;
        if (uri != null) {
            Log.e("--getData--", uri + "");
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                confirmPermission();
                apkPath = uri.getPath();
            } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                apkPath = createApkFromUri(this);
            } else {
                showToast(getString(R.string.failed_prase));
            }
            return apkPath;
        } else {
            finish();
            return "";
        }
    }

    protected abstract void startInstall(String apkPath);

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0x233);
    }

    private void confirmPermission() {
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean judge = (permissionRead == 0);
        if (!judge) {
            requestPermission();
        }
    }


    void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    private String createApkFromUri(Context context) {
        istemp = true;
        File tempFile = new File(context.getExternalCacheDir(), System.currentTimeMillis() + ".apk");
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is != null) {
                OutputStream fos = new FileOutputStream(tempFile);
                byte[] buf = new byte[4096 * 1024];
                int ret;
                while ((ret = is.read(buf)) != -1) {
                    fos.write(buf, 0, ret);
                    fos.flush();
                }
                fos.close();
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile.getAbsolutePath();
    }


    void deleteSingleFile(File file) {
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.e("-DELETE-", "==>" + file.getAbsolutePath() + " OK！");
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    String[] getApkPkgInfo(String apkPath) {
        if (apkPath == null) {
            return null;
        } else {
            PackageManager pm = this.getPackageManager();
            PackageInfo pkgInfo = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
            if (pkgInfo != null) {
                pkgInfo.applicationInfo.sourceDir = apkPath;
                pkgInfo.applicationInfo.publicSourceDir = apkPath;

                return new String[]{pkgInfo.packageName, pm.getApplicationLabel(pkgInfo.applicationInfo).toString() + "_" + pkgInfo.versionName + "(" + pkgInfo.versionCode + ")"};
            }
            return null;
        }
    }

}
