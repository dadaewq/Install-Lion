package com.modosa.apkinstaller.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.utils.ApkInfo;
import com.modosa.apkinstaller.utils.apksource.ApkSource;
import com.modosa.apkinstaller.utils.installer.ApkSourceBuilder;
import com.modosa.apkinstaller.utils.installer.SAIPackageInstaller;
import com.modosa.apkinstaller.utils.installer.shizuku.ShizukuSAIPackageInstaller;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * @author dadaewq
 */
public class Install3Activity extends Activity implements com.modosa.apkinstaller.utils.installer.SAIPackageInstaller.InstallationStatusListener {
    private static final String TAG = "method";
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private boolean istemp = false;
    private Uri uri;
    private boolean needrequest;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String[] apkinfo;
    private boolean is_show;
    private long mOngoingSessionId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        uri = intent.getData();

        needrequest = (Build.VERSION.SDK_INT >= 23) && ((uri + "").contains("file://"));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        is_show = sharedPreferences.getBoolean("show_notification", false);

        init();

    }

    private void init() {
        String apkPath;
        boolean needconfirm = sharedPreferences.getBoolean("needconfirm", true);

        apkPath = preInstall();
        apkinfo = new ApkInfo(this, apkPath).getApkPkgInfo();
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
                startInstall(apkPath);
                finish();
            });
            builder.setNegativeButton(android.R.string.no, (dialog, which) -> finish());
            builder.setCancelable(false);
            builder.show();


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

    private String preInstall() {
        String apkPath = null;
        if (uri != null) {
            Log.e("--getData--", uri + "");
            String CONTENT = "content://";
            String FILE = "file://";
            if (uri.toString().contains(FILE)) {
                confirmPermission();
                apkPath = uri.getPath();
            } else if (uri.toString().contains(CONTENT)) {
                apkPath = createApkFromUri(this);
            }
            return apkPath;
        } else {
            finish();
            return "";
        }
    }

    private void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            final File apkFile = new File(apkPath);

            apkinfo = new ApkInfo(this, apkPath).getApkPkgInfo();

            ArrayList<File> files = new ArrayList<>();
            files.add(apkFile);
            new Thread(() -> {
                showToast(getString(R.string.install_start) + apkinfo[1]);
                try {
                    installPackages(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (istemp) {
                    deleteSingleFile(apkFile);
                }
                finish();
            }

            ).start();
        } else {
            showToast(getString(R.string.failed_read));
            finish();
        }
    }

    private void installPackages(List<File> apkFiles) {
        Log.d(TAG, "installPackages: ");

        Context mContext = getApplication();
        SAIPackageInstaller mInstaller = ShizukuSAIPackageInstaller.getInstance(mContext);
        mInstaller.addStatusListener(this);
        ApkSource apkSource = new ApkSourceBuilder()
                .fromApkFiles(apkFiles)
                .build();

        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
    }

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

    private void showToast(final String text) {
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

    private void deleteSingleFile(File file) {
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.e("--DELETE--", "deleteSingleFile" + file.getAbsolutePath() + " OK！");
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onStatusChanged(long installationID, SAIPackageInstaller.InstallationStatus status, @Nullable String packageNameOrErrorDescription) {
        if (installationID != mOngoingSessionId) {
            return;
        }
        Log.e("status", status + "");
        switch (status) {
            case QUEUED:
            case INSTALLING:
                break;
            case INSTALLATION_SUCCEED:
                showToast(getString(R.string.success_install));
                if (is_show) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".activity.NotifyActivity"));
                    Log.e("packagename", apkinfo[0]);

                    intent.putExtra("channelId", "3");
                    intent.putExtra("channelName", getString(R.string.name_install3));
                    intent.putExtra("packageName", apkinfo[0]);
                    intent.putExtra("packageLable", apkinfo[1]);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                break;
            case INSTALLATION_FAILED:
                finish();
                showToast(getString(R.string.failed_install));
                break;
            default:
                finish();
        }
    }

}
