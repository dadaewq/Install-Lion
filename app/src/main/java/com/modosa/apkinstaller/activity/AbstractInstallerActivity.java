package com.modosa.apkinstaller.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.fragment.SettingsFragment;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.NotifyUtil;
import com.modosa.apkinstaller.util.OpUtil;
import com.modosa.apkinstaller.util.PraseContentUtil;

import java.io.File;
import java.util.Objects;


/**
 * @author dadaewq
 */
public abstract class AbstractInstallerActivity extends AppCompatActivity {
    private static final int REQUEST_REFRESH_WRITE_PERMISSION = 0x2330;
    private static final int REQUEST_PICK_APK_FILE = 0x2331;
    private static final String ILLEGALPKGNAME = "IL^&IllegalPN*@!128`+=：:,.[";
    private final String[] writePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final String nl = System.getProperty("line.separator");
    String[] apkinfo;
    String uninstallPackageLable;
    StringBuilder alertDialogMessage;
    File installApkFile;
    boolean show_notification;
    boolean enableAnotherinstaller;
    boolean istemp = false;
    private String validApkPath;
    private boolean isInstalledSuccess = false;
    private boolean deleteSucceededApk;
    private String[] source;
    private Uri uri;
    private SharedPreferences spAllowSource;
    private SharedPreferences spGetPreferenceManager;
    private AlertDialog alertDialog;
    private String cachePath;
    private String uninstallPkgName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFromAction(getIntent().getAction() + "");
    }


    private void initFromAction(String action) {
        switch (action) {
            case OpUtil.MODOSA_ACTION_PICK_FILE:
                openFile();
                break;
            case Intent.ACTION_DELETE:
            case Intent.ACTION_UNINSTALL_PACKAGE:
                uninstallPkgName = Objects.requireNonNull(getIntent().getData()).getEncodedSchemeSpecificPart();
                if (uninstallPkgName == null) {
                    showMyToast0(R.string.tip_failed_prase);
                    finish();
                } else {
                    initUninstall();
                }
                break;
            default:
                uri = getIntent().getData();
                initFromUri();

        }
    }

    // 用 ACTION_GET_CONTENT 而不是ACTION_OPEN_DOCUMENT 可支持更多App
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 因为设备上Apk文件的后缀并不一定是"apk"，所以不使用"application/vnd.android.package-archive"
        intent.setType("application/*");
        startActivityForResult(intent, REQUEST_PICK_APK_FILE);
    }

    private void initFromUri() {
        spAllowSource = getSharedPreferences("allowsource", Context.MODE_PRIVATE);
        spGetPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this);
        if (checkPermission()) {
            initInstall();
        } else {
            requestPermission();
        }
    }

    private void initUninstall() {
        String[] version = AppInfoUtil.getApplicationVersion(this, uninstallPkgName);

        uninstallPackageLable = AppInfoUtil.getApplicationLabel(this, uninstallPkgName);
        if (AppInfoUtil.UNINSTALLED.equals(uninstallPackageLable)) {
            uninstallPackageLable = "Uninstalled";
        }

        View infoView = View.inflate(this, R.layout.uninstall_content_view, null);

        TextView textView1 = infoView.findViewById(R.id.textView1);
        TextView textView2 = infoView.findViewById(R.id.textView2);

        String text1 = uninstallPkgName;
        String text2;

        textView1.setText(text1);
        textView1.setOnClickListener(view -> {
            copyErr(text1);
            showMyToast0(R.string.tip_copy_to_clipboard);
        });


        if (version == null) {
            text2 = getString(R.string.unknown);
        } else {
            text2 = version[0] + " (" + version[1] + ")";
        }
        textView2.setText(text2);

        textView2.setOnClickListener(view -> {
            copyErr(text2);
            showMyToast0(R.string.tip_copy_to_clipboard);
        });

        alertDialogMessage = new StringBuilder().append(uninstallPackageLable).append(nl).append(text1).append(nl).append(text2);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setIcon(AppInfoUtil.getApplicationIconDrawable(this, uninstallPkgName))
                .setTitle(uninstallPackageLable)
                .setView(infoView)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> finish())
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    startUninstall(uninstallPkgName);
                    finish();
                });
        alertDialog = builder.create();
        OpUtil.showAlertDialog(this, alertDialog);
        alertDialog.setOnCancelListener(dialog -> finish());
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);

    }

    private void initInstall() {
        source = checkInstallSource();
        boolean needconfirm = spGetPreferenceManager.getBoolean("needconfirm", true);
        deleteSucceededApk = spGetPreferenceManager.getBoolean("deleteSucceededApk", false);
        show_notification = spGetPreferenceManager.getBoolean("show_notification", false);
        enableAnotherinstaller = spGetPreferenceManager.getBoolean(SettingsFragment.SP_KEY_ENABLE_ANOTHER_INSTALLER, false);
        boolean allowsource = spAllowSource.getBoolean(source[0], false);

        validApkPath = preInstallGetValidApkPath();
        if (validApkPath == null) {
            showMyToast0(R.string.tip_failed_prase);
            finish();
        } else {
            cachePath = validApkPath;
            Log.e("cachePath", cachePath + "");

            if (needconfirm) {
                if (!source[1].equals(ILLEGALPKGNAME) && allowsource) {
                    startInstall(validApkPath);
                    finish();
                    return;
                }
            } else {
                startInstall(validApkPath);
                finish();
                return;
            }
            String[] appVersion = AppInfoUtil.getApplicationVersion(this, apkinfo[1]);

            View infoView = View.inflate(this, R.layout.install_content_view, null);

            TextView textView1 = infoView.findViewById(R.id.textView1);
            TextView textView2 = infoView.findViewById(R.id.textView2);
            TextView textView3 = infoView.findViewById(R.id.textView3);
            TextView textView4 = infoView.findViewById(R.id.textView4);
            CheckBox checkBox = infoView.findViewById(R.id.confirm_checkbox);

            String text1 = apkinfo[1];
            String text2 = apkinfo[2] + " (" + apkinfo[3] + ")";
            String text3 = null;
            String text4 = apkinfo[4];

            textView1.setText(text1);
            textView1.setOnClickListener(view -> {
                copyErr(text1);
                showMyToast0(R.string.tip_copy_to_clipboard);
            });

            textView2.setText(text2);

            textView2.setOnClickListener(view -> {
                copyErr(text2);
                showMyToast0(R.string.tip_copy_to_clipboard);
            });

            if (appVersion != null) {
                infoView.findViewById(R.id.line3).setVisibility(View.VISIBLE);
                text3 = appVersion[0] + " (" + appVersion[1] + ")";
                Long appVersionCode = Long.parseLong(appVersion[1]);
                Long apkVersionCode = Long.parseLong(apkinfo[3]);

                String text2Suffix = "";
                if (apkVersionCode > appVersionCode) {
                    // ↗
                    text2Suffix = " \u2197";
                } else if (apkVersionCode < appVersionCode) {
                    // ↘
                    text2Suffix = " \u2198";
                }
                textView2.append(text2Suffix);
                textView3.setText(text3);
                String finalText = text3;
                textView3.setOnClickListener(view -> {
                    copyErr(finalText);
                    showMyToast0(R.string.tip_copy_to_clipboard);
                });
            }

            textView4.setText(text4);
            textView4.setOnClickListener(view -> {
                copyErr(text4);
                showMyToast0(R.string.tip_copy_to_clipboard);
            });


            if (source[1].equals(ILLEGALPKGNAME)) {
                checkBox.setText(R.string.checkbox_installsource_unkonwn);
                checkBox.setEnabled(false);
            } else {
                checkBox.setText(String.format(getString(R.string.checkbox_always_allow), source[1]));
            }

            if (appVersion != null) {
                alertDialogMessage = new StringBuilder().append(apkinfo[0]).append(nl).append(text1).append(nl).append(text2).append(nl).append(text3).append(nl).append(text4);
            } else {
                alertDialogMessage = new StringBuilder().append(apkinfo[0]).append(nl).append(text1).append(nl).append(text2).append(nl).append(text4);
            }


            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setIcon(AppInfoUtil.getApkIconDrawable(this, validApkPath))
                    .setTitle(apkinfo[0])
                    .setView(infoView)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        cachePath = null;
                        if (!source[1].equals(ILLEGALPKGNAME)) {
                            spAllowSource.edit().putBoolean(source[0], checkBox.isChecked()).apply();
                        }
                        startInstall(validApkPath);
                        finish();
                    });

            alertDialog = builder.create();
            OpUtil.showAlertDialog(this, alertDialog);


            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
        }
    }


//    private void initUninstall1() {
//        String[] version = AppInfoUtil.getApplicationVersion(this, uninstallPkgName);
//
//        uninstallPackageLable = AppInfoUtil.getApplicationLabel(this, uninstallPkgName);
//        if (AppInfoUtil.UNINSTALLED.equals(uninstallPackageLable)) {
//            uninstallPackageLable = "Uninstalled";
//        }
//
//        View infoView = View.inflate(this, R.layout.uninstall_content_viewuu, null);
//
//        TextView textView1 = infoView.findViewById(R.id.textView1);
//        TextView textView2 = infoView.findViewById(R.id.textView2);
//
//        String text1 = uninstallPkgName;
//        String text2;
//
//        textView1.setText(String.format(
//                getString(R.string.tv_packagename),
//                text1
//        ));
//        textView1.setOnClickListener(view -> {
//            copyErr(text1);
//            showMyToast0(text1);
//        });
//
//
//        if (version == null) {
//            text2 = getString(R.string.unknown);
//        } else {
//            text2 = version[0] + " (" + version[1] + ")";
//        }
//        textView2.setText(String.format(
//                getString(R.string.tv_version_apk),
//                text2));
//
//        textView2.setOnClickListener(view -> {
//            copyErr(text2);
//            showMyToast0(text2);
//        });
//
//        alertDialogMessage = new StringBuilder().append(String.format(getString(R.string.tv_lable), uninstallPackageLable)).append(nl).append(text1).append(nl).append(text2);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setIcon(AppInfoUtil.getApplicationIconDrawable(this, uninstallPkgName))
//                .setTitle(uninstallPackageLable)
//                .setView(infoView)
//                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> finish())
//                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
//                    startUninstall(uninstallPkgName);
//                    finish();
//                });
//        alertDialog = builder.create();
//        OpUtil.showAlertDialog(this, alertDialog);
//        alertDialog.setOnCancelListener(dialog -> finish());
//        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
//        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
//
//    }
//
//
//
//    private void initInstall1() {
//        source = checkInstallSource();
//        boolean needconfirm = spGetPreferenceManager.getBoolean("needconfirm", true);
//        deleteSucceededApk = spGetPreferenceManager.getBoolean("deleteSucceededApk", false);
//        show_notification = spGetPreferenceManager.getBoolean("show_notification", false);
//        enableAnotherinstaller = spGetPreferenceManager.getBoolean(SettingsFragment.SP_KEY_ENABLE_ANOTHER_INSTALLER, false);
//        boolean allowsource = spAllowSource.getBoolean(source[0], false);
//
//        validApkPath = preInstallGetValidApkPath();
//        if (validApkPath == null) {
//            showMyToast0(R.string.tip_failed_prase);
//            finish();
//        } else {
//            cachePath = validApkPath;
//            Log.e("cachePath", cachePath + "");
//
//            if (needconfirm) {
//                if (!source[1].equals(ILLEGALPKGNAME) && allowsource) {
//                    startInstall(validApkPath);
//                    finish();
//                    return;
//                }
//            } else {
//                startInstall(validApkPath);
//                finish();
//                return;
//            }
//            String[] appVersion = AppInfoUtil.getApplicationVersion(this, apkinfo[1]);
//
//            View infoView = View.inflate(this, R.layout.install_content_viewuu, null);
//
//            TextView textView1 = infoView.findViewById(R.id.textView1);
//            TextView textView2 = infoView.findViewById(R.id.textView2);
//            TextView textView3 = infoView.findViewById(R.id.textView3);
//            TextView textView4 = infoView.findViewById(R.id.textView4);
//            CheckBox checkBox = infoView.findViewById(R.id.confirm_checkbox);
//
//            String text1 = apkinfo[1];
//            String text2 = apkinfo[2] + " (" + apkinfo[3] + ")";
//            String text3 = null;
//            String text4 = apkinfo[4];
//
//            textView1.setText(String.format(
//                    getString(R.string.tv_packagename),
//                    text1
//            ));
//            textView1.setOnClickListener(view -> {
//                copyErr(text1);
//                showMyToast0(text1);
//            });
//
//            textView2.setText(String.format(
//                    getString(R.string.tv_version_apk),
//                    text2));
//
//            textView2.setOnClickListener(view -> {
//                copyErr(text2);
//                showMyToast0(text2);
//            });
//
//            if (appVersion != null) {
//                textView3.setVisibility(View.VISIBLE);
//                text3 = appVersion[0] + " (" + appVersion[1] + ")";
//                Long appVersionCode = Long.parseLong(appVersion[1]);
//                Long apkVersionCode = Long.parseLong(apkinfo[3]);
//
//                String text2Suffix = "";
//                if (apkVersionCode > appVersionCode) {
//                    // ↗
//                    text2Suffix = " \u2197";
//                } else if (apkVersionCode < appVersionCode) {
//                    // ↘
//                    text2Suffix = " \u2198";
//                }
//                textView2.append(text2Suffix);
//                textView3.setText(String.format(
//                        getString(R.string.tv_version_app),
//                        text3
//                        )
//                );
//                String finalText = text3;
//                textView3.setOnClickListener(view -> {
//                    copyErr(finalText);
//                    showMyToast0(finalText);
//                });
//            }
//
//            textView4.setText(String.format(
//                    getString(R.string.tv_size_apk),
//                    text4
//            ));
//            textView4.setOnClickListener(view -> {
//                copyErr(text4);
//                showMyToast0(text4);
//            });
//
//
//            if (source[1].equals(ILLEGALPKGNAME)) {
//                checkBox.setText(R.string.checkbox_installsource_unkonwn);
//                checkBox.setEnabled(false);
//            } else {
//                checkBox.setText(String.format(getString(R.string.checkbox_always_allow), source[1]));
//            }
//
//            if (appVersion != null) {
//                alertDialogMessage = new StringBuilder().append(String.format(getString(R.string.tv_lable), apkinfo[0])).append(nl).append(text1).append(nl).append(text2).append(nl).append(text3).append(nl).append(text4);
//            } else {
//                alertDialogMessage = new StringBuilder().append(String.format(getString(R.string.tv_lable), apkinfo[0])).append(nl).append(text1).append(nl).append(text2).append(nl).append(text4);
//            }
//
//
//            AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                    .setIcon(AppInfoUtil.getApkIconDrawable(this, validApkPath))
//                    .setTitle(apkinfo[0])
//                    .setView(infoView)
//                    .setCancelable(false)
//                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> finish())
//                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
//                        cachePath = null;
//                        if (!source[1].equals(ILLEGALPKGNAME)) {
//                            spAllowSource.edit().putBoolean(source[0], checkBox.isChecked()).apply();
//
//                        }
//                        startInstall(validApkPath);
//                        finish();
//                    });
//
//            alertDialog = builder.create();
//            OpUtil.showAlertDialog(this, alertDialog);
//
//            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
//            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
//        }
//    }


    private String[] checkInstallSource() {
        final String fromPkgLabel;
        final String fromPkgName;

        String referrer = PraseContentUtil.reflectGetReferrer(this);
        if (referrer != null) {
            fromPkgName = referrer;
        } else {
            Uri referrerUri = getReferrer();
            if (referrerUri == null || !"android-app".equals(referrerUri.getScheme())) {
                fromPkgLabel = ILLEGALPKGNAME;
                fromPkgName = ILLEGALPKGNAME;
                return new String[]{fromPkgName, fromPkgLabel};
            } else {
                fromPkgName = referrerUri.getEncodedSchemeSpecificPart().substring(2);
            }
        }
        String refererPackageLabel =
                AppInfoUtil.getApplicationLabel(this, fromPkgName);
        if (AppInfoUtil.UNINSTALLED.equals(refererPackageLabel)) {
            fromPkgLabel = ILLEGALPKGNAME;
        } else {
            fromPkgLabel = refererPackageLabel;
        }
        return new String[]{fromPkgName, fromPkgLabel};
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == REQUEST_PICK_APK_FILE
                && resultCode == Activity.RESULT_OK
                && resultData != null) {

            uri = resultData.getData();
            initFromUri();
        } else {
            showMyToast0(R.string.tip_failed_get_content);
            finish();
        }

    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (istemp && (cachePath != null) && !enableAnotherinstaller) {
            OpUtil.deleteSingleFile(new File(cachePath));
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (checkPermission()) {
            initInstall();
        } else {
            requestPermission();
        }
    }

    private String preInstallGetValidApkPath() {
        String getPath = null;
        Log.e("uri", uri + "");
        if (uri != null) {
            Log.e("--getData--", uri + "");
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                getPath = uri.getPath();
            } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {

                File file = PraseContentUtil.getFile(this, uri);
                if (file != null) {
                    getPath = file.getPath();
                } else {
                    istemp = true;
                    getPath = OpUtil.createApkFromUri(this, uri).getPath();
                }
                Log.e("getPathfromContent", getPath + "");
            }
            if (getPath != null) {
                apkinfo = AppInfoUtil.getApkInfo(this, getPath);
                if (apkinfo != null) {
                    return getPath;
                }
            }

        }

        if (getPath != null && istemp) {
            OpUtil.deleteSingleFile(new File(getPath));
        }
        return null;
    }

    protected abstract void startInstall(String installApkFile);

    protected abstract void startUninstall(String uninstallPkgname);

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, writePermissions, REQUEST_REFRESH_WRITE_PERMISSION);
    }

    private boolean checkPermission() {
        int checkSelfPermission = ContextCompat.checkSelfPermission(this, writePermissions[0]);
        return (checkSelfPermission == 0);
    }

    void copyErr(String err) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, err);
        Objects.requireNonNull(clipboard).setPrimaryClip(clipData);
    }

    void showMyToast0(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    void showMyToast0(final int stringId) {
        runOnUiThread(() -> Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show());
    }

    void showMyToast1(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }

    void showMyToast1(final int stringId) {
        runOnUiThread(() -> Toast.makeText(this, stringId, Toast.LENGTH_LONG).show());
    }


    void deleteCache() {
        boolean shouDelete = deleteSucceededApk && isInstalledSuccess;
        if (istemp || shouDelete) {
            OpUtil.deleteSingleFile(installApkFile);
        }
    }

    void showNotificationWithdeleteCache(String channelId, boolean success) {
        if (success) {
            isInstalledSuccess = true;
            deleteCache();
            if (show_notification) {
                Log.e("packagename", apkinfo[1]);
                new NotifyUtil(this).sendSuccessNotification(channelId, String.format(getString(R.string.tip_success_install), apkinfo[0]), apkinfo[1]);
            }

        } else {
            isInstalledSuccess = false;
            if (show_notification) {
                Log.e("packagename", apkinfo[1]);
                new NotifyUtil(this).sendFailNotification(NotifyUtil.CHANNEL_ID_FAIL, String.format(getString(R.string.tip_failed_install), apkinfo[0]), apkinfo[1], validApkPath, istemp && !enableAnotherinstaller);
            } else {
                if (!enableAnotherinstaller) {
                    deleteCache();
                }
            }
            if (enableAnotherinstaller) {
                OpUtil.startAnotherInstaller(this, installApkFile, istemp);
            }
        }
    }
}
