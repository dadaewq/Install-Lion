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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.OpUtil;
import com.modosa.apkinstaller.util.PraseContentUtil;

import java.io.File;
import java.util.Objects;


/**
 * @author dadaewq
 */
public abstract class AbstractInstallerActivity extends Activity {

    private static final int PICK_APK_FILE = 2;
    private static final String ILLEGALPKGNAME = "IL^&IllegalPN*@!128`+=：:,.[";
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private final String nl = System.getProperty("line.separator");
    String[] apkinfo;
    String uninstallPackageLable;
    StringBuilder alertDialogMessage;
    File installApkFile;
    boolean show_notification;
    boolean istemp = false;
    private String[] source;
    private Uri uri;
    private SharedPreferences sourceSp;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
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

    private void initFromUri() {
        sourceSp = getSharedPreferences("allowsource", Context.MODE_PRIVATE);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
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
        alertDialogMessage = new StringBuilder();
        alertDialogMessage
                .append(
                        String.format(
                                getString(R.string.message_name),
                                uninstallPackageLable
                        )
                )
                .append(nl)
                .append(
                        String.format(
                                getString(R.string.message_packagename),
                                uninstallPkgName
                        )
                )
                .append(nl);

        if (version != null) {
            alertDialogMessage.append(String.format(
                    getString(R.string.message_version),
                    version[0],
                    version[1])
            )
                    .append(nl);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.title_dialog_uninstall)
                .setMessage(alertDialogMessage + nl + nl + getString(R.string.message_irrevocableConfirm))
                .setNegativeButton(android.R.string.no, (dialogInterface, i) -> finish())
                .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
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
        boolean needconfirm = sharedPreferences.getBoolean("needconfirm", true);
        show_notification = sharedPreferences.getBoolean("show_notification", false);
        boolean allowsource = sourceSp.getBoolean(source[0], false);
        String validApkPath = preInstallGetValidApkPath();
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
            String[] version = AppInfoUtil.getApplicationVersion(this, apkinfo[1]);

            alertDialogMessage = new StringBuilder();
            alertDialogMessage
                    .append(nl)
                    .append(
                            String.format(
                                    getString(R.string.message_name),
                                    apkinfo[0]
                            )
                    )
                    .append(nl)
                    .append(
                            String.format(
                                    getString(R.string.message_packagename),
                                    apkinfo[1]
                            )
                    )
                    .append(nl)
                    .append(
                            String.format(
                                    getString(R.string.message_version),
                                    apkinfo[2],
                                    apkinfo[3]
                            )
                    )
                    .append(nl);

            if (version != null) {
                alertDialogMessage.append(
                        String.format(
                                getString(R.string.message_version_existed),
                                version[0],
                                version[1]
                        )
                )
                        .append(nl);
            }

            alertDialogMessage
                    .append(
                            String.format(
                                    getString(R.string.message_size),
                                    apkinfo[4]
                            )
                    )
                    .append(nl);


            View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);

            CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);

            if (source[1].equals(ILLEGALPKGNAME)) {
                checkBox.setText(R.string.checkbox_installsource_unkonwn);
                checkBox.setEnabled(false);
            } else {
                checkBox.setText(String.format(getString(R.string.checkbox_always_allow), source[1]));
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_dialog_install)
                    .setMessage(alertDialogMessage)
                    .setView(checkBoxView)
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.no, (dialog, which) -> finish())
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        cachePath = null;
                        if (!source[1].equals(ILLEGALPKGNAME)) {
                            editor = sourceSp.edit();
                            editor.putBoolean(source[0], checkBox.isChecked());
                            editor.apply();
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

        if (requestCode == PICK_APK_FILE
                && resultCode == Activity.RESULT_OK
                && resultData != null) {

            uri = resultData.getData();
            initFromUri();
        } else {
            showMyToast0(R.string.tip_failed_get_content);
            finish();
        }

    }

    // 用 ACTION_GET_CONTENT 而不是ACTION_OPEN_DOCUMENT 可支持更多App
    private void openFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // 因为设备上Apk文件的后缀并不一定是"apk"，所以不使用"application/vnd.android.package-archive"
        intent.setType("application/*");
        startActivityForResult(intent, PICK_APK_FILE);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (istemp && (cachePath != null)) {
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
        ActivityCompat.requestPermissions(this, permissions, 0x2330);
    }

    private boolean checkPermission() {
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return (permissionRead == 0);
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
        if (istemp) {
            OpUtil.deleteSingleFile(installApkFile);
        }
    }
}
