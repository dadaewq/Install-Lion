package com.modosa.apkinstaller.activity;

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
import androidx.preference.PreferenceManager;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.fragment.SettingsFragment;
import com.modosa.apkinstaller.util.AppInfoUtil;
import com.modosa.apkinstaller.util.NotifyUtil;
import com.modosa.apkinstaller.util.OpUtil;
import com.modosa.apkinstaller.util.PraseContentUtil;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import me.rosuh.filepicker.config.FilePickerManager;


/**
 * @author dadaewq
 */
public abstract class AbstractInstallerActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_APK_FILE = 0x2331;
    private static final String ILLEGALPKGNAME = "IL^&IllegalPN*@!128`+=：:,.[";
    private final String nl = System.getProperty("line.separator");
    String[] apkinfo;
    String uninstallPackageLable;
    StringBuilder alertDialogMessage;
    File installApkFile;
    boolean istemp = false;
    private boolean show_notification;
    private boolean enableAnotherinstaller;
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
        spGetPreferenceManager = PreferenceManager.getDefaultSharedPreferences(this);
        initFromAction(getIntent().getAction() + "");
    }

    private void initFromAction(String action) {

        switch (action) {
            case OpUtil.MODOSA_ACTION_PICK_FILE:
                if (spGetPreferenceManager.getBoolean("useInternalFilePicker", true)) {
                    getFile();
                } else {
                    getContent();
                }
                break;
            case OpUtil.MODOSA_ACTION_GO_OPEN_DOCUMENT:
                openDocument();
                break;
            case OpUtil.MODOSA_ACTION_GO_GET_FILE:
                getFile();
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


    private void startGetFile() {

        Intent intent = new Intent(OpUtil.MODOSA_ACTION_GO_GET_FILE)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setClass(this, getClass());
        startActivity(intent);

    }

    private void getFile() {

        FilePickerManager.INSTANCE.saveData(new ArrayList<>());
        FilePickerManager.INSTANCE
                .from(this)
                .enableSingleChoice()
                .showHiddenFiles(true)
                .setTheme(R.style.myFilePickerThemeRail)
                .forResult(FilePickerManager.REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Log.e("onActivityResult", requestCode + "|" + resultCode + "|\n" + resultData);
        switch (requestCode) {
            case FilePickerManager.REQUEST_CODE:
                List<String> list = FilePickerManager.INSTANCE.obtainData();

                if (list.size() != 0) {
                    String path = list.get(0);
                    uri = Uri.fromFile(new File(path));
                    initFromUri();
                } else {
                    finish();
                }
                break;
            case REQUEST_PICK_APK_FILE:
                if (resultData != null) {
                    uri = resultData.getData();
                    initFromUri();
                } else {
                    showMyToast0(R.string.tip_failed_get_content);
                    finish();
                    break;
                }
            default:
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (OpUtil.checkWritePermission(this)) {
            initInstall();
        } else {
            OpUtil.requestWritePermission(this);
        }
    }

    /**
     * 优先用 ACTION_GET_CONTENT可支持更多App
     */
    private void getContent() {
        Intent intentContent = new Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                // 因为设备上Apk文件的后缀并不一定是"apk"，所以不使用"application/vnd.android.package-archive"
                .setType("*/*");
        try {
            startActivityForResult(intentContent, REQUEST_PICK_APK_FILE);
        } catch (Exception e0) {
            e0.printStackTrace();
            startOpenDocument();
        }
    }

    private void startOpenDocument() {

        Intent intent = new Intent(OpUtil.MODOSA_ACTION_GO_OPEN_DOCUMENT)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setClass(this, getClass());
        startActivity(intent);

    }

    /**
     * 部分设备没有可以处理GET_CONTENT的Activity 所以再次尝试ACTION_OPEN_DOCUMENT
     */
    private void openDocument() {
        Intent intentDocument = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*");
        try {
            startActivityForResult(intentDocument, REQUEST_PICK_APK_FILE);
        } catch (Exception e0) {
            e0.printStackTrace();
            startGetFile();
        }
    }


    private void initFromUri() {
        spAllowSource = getSharedPreferences(ManageAllowSourceActivity.SP_KEY_ALLOWSOURCE, Context.MODE_PRIVATE);
        if (OpUtil.checkWritePermission(this)) {
            initInstall();
        } else {
            OpUtil.requestWritePermission(this);
        }
    }

    private void initUninstall() {
        String[] version = AppInfoUtil.getApplicationVersion(this, uninstallPkgName);

        uninstallPackageLable = AppInfoUtil.getApplicationLabel(this, uninstallPkgName);
//        if (AppInfoUtil.UNINSTALLED.equals(uninstallPackageLable)) {
//            uninstallPackageLable = "Uninstalled";
//        }

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

        boolean skipConfirm = true;
        boolean needconfirm = spGetPreferenceManager.getBoolean("needconfirm", true);
        boolean isAllowsource = false;
        String allowsourceString = spAllowSource.getString(ManageAllowSourceActivity.SP_KEY_ALLOWSOURCE, "");
        List<String> allowsourceList = OpUtil.convertToList(allowsourceString, ",");
        if (!source[1].equals(ILLEGALPKGNAME)) {
            isAllowsource = allowsourceList.contains(source[0]);
        }

        if (needconfirm && !isAllowsource) {
            skipConfirm = false;
        }

        validApkPath = preInstallGetValidApkPath();
        if (validApkPath == null) {
            showMyToast0(R.string.tip_failed_prase);
            finish();
        } else {
            cachePath = validApkPath;
            Log.e("cachePath", cachePath + "");

            show_notification = spGetPreferenceManager.getBoolean("show_notification", false);
            deleteSucceededApk = spGetPreferenceManager.getBoolean("deleteSucceededApk", false);
            enableAnotherinstaller = spGetPreferenceManager.getBoolean(SettingsFragment.SP_KEY_ENABLE_ANOTHER_INSTALLER, false);

            if (skipConfirm) {
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
                    .setPositiveButton(R.string.bt_install, (dialog, which) -> {
                        cachePath = null;
                        if (!source[1].equals(ILLEGALPKGNAME) && checkBox.isChecked()) {
                            spAllowSource.edit().putString(ManageAllowSourceActivity.SP_KEY_ALLOWSOURCE, allowsourceString + source[0] + ",").apply();
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
        if (getString(R.string.unknown).equals(refererPackageLabel)) {
            fromPkgLabel = ILLEGALPKGNAME;
        } else {
            fromPkgLabel = refererPackageLabel;
        }
        return new String[]{fromPkgName, fromPkgLabel};
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
                    InputStream is = null;
                    try {
                        is = getContentResolver().openInputStream(uri);
                    } catch (Exception e) {
                        copyErr(uri + "\nopenInputStream " + e);
                        showMyToast1("致命错误，请向开发者报告\n" + uri + "\n" + e + "\n已复制到剪贴板");
                        e.printStackTrace();
                        finish();
                    }
                    if (is == null) {
                        copyErr(uri + "\nis = null");
                        showMyToast1("致命错误，请向开发者报告\n" + uri + "\n已复制到剪贴板");
                        finish();
                    }

                    if (is != null) {
                        getPath = OpUtil.createApkFromUri(this, is).getPath();
                    }
                    Log.e("createApkFromUri", getPath + "");

                }

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

    /**
     * 开始安装 installApkFile
     */
    protected abstract void startInstall(String installApkFile);

    /**
     * 开始卸载 uninstallPkgname
     */
    protected abstract void startUninstall(String uninstallPkgname);

//    private void requestPermission() {
//        try {
//            ActivityCompat.requestPermissions(this, writePermissions, REQUEST_REFRESH_WRITE_PERMISSION);
//        } catch (Exception e) {
//            showMyToast1("" + e);
//            finish();
//        }
//    }

//    private boolean checkPermission() {
//        int checkSelfPermission = ContextCompat.checkSelfPermission(this, writePermissions[0]);
//        return (checkSelfPermission == 0);
//    }

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
        Log.e("installed " + (success ? "success" : "fail"), apkinfo[1]);
        isInstalledSuccess = success;
        if (isInstalledSuccess) {
            deleteCache();
            if (show_notification) {
                new NotifyUtil(this).sendNotification(channelId, String.format(getString(R.string.tip_success_install), apkinfo[0]), apkinfo[1], validApkPath, istemp, enableAnotherinstaller);
            }
        } else {
            if (show_notification) {
                new NotifyUtil(this).sendNotification(NotifyUtil.CHANNEL_ID_FAIL, String.format(getString(R.string.tip_failed_install), apkinfo[0]), apkinfo[1], validApkPath, istemp, enableAnotherinstaller);
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
