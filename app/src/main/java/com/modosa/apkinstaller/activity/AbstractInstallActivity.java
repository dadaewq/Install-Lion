package com.modosa.apkinstaller.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.utils.AppInfoUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Objects;


/**
 * @author dadaewq
 */
public abstract class AbstractInstallActivity extends Activity {
    private static final String ILLEGALPKGNAME = "IL^&IllegalPN*@!128`+=：:,.[";
    private final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    private final String nl = System.getProperty("line.separator");
    String packageLable;
    String[] apkinfo;
    StringBuilder alertDialogMessage;
    File apkFile;
    boolean show_notification;
    private boolean istemp = false;
    private String[] source;
    private Uri uri;
    private boolean needrequest;
    private SharedPreferences sourceSp;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private AlertDialog alertDialog;
    private String cachePath;
    private String pkgName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String action = getIntent().getAction();
        if (Intent.ACTION_DELETE.equals(action) || Intent.ACTION_UNINSTALL_PACKAGE.equals(action)) {
            pkgName = Objects.requireNonNull(getIntent().getData()).getEncodedSchemeSpecificPart();
            if (pkgName == null) {
                showToast0(getString(R.string.failed_prase));
                finish();
            } else {
                initUninstall();
            }
        } else {
            uri = getIntent().getData();

            assert uri != null;
            needrequest = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) && (ContentResolver.SCHEME_FILE.equals(uri.getScheme()));

            sourceSp = getSharedPreferences("allowsource", Context.MODE_PRIVATE);
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (needrequest) {
                if (checkPermission()) {
                    needrequest = false;
                    initInstall();
                } else {
                    requestPermission();
                }
            } else {
                initInstall();
            }
        }

    }



    private void initUninstall() {
        String[] version = AppInfoUtils.getApplicationVersion(this, pkgName);

        packageLable = AppInfoUtils.getApplicationLabel(this, pkgName);
        if (AppInfoUtils.UNINSTALLED.equals(packageLable)) {
            packageLable = "Uninstalled";
        }
        alertDialogMessage = new StringBuilder();
        alertDialogMessage
                .append(
                        String.format(
                                getString(R.string.message_name),
                                packageLable
                        )
                )
                .append(nl)
                .append(
                        String.format(
                                getString(R.string.message_packagename),
                                pkgName
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_uninstall_title));
        builder.setMessage(alertDialogMessage + nl + nl + getString(R.string.message_uninstalConfirm));
        builder.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> {
            startUninstall(pkgName);
            finish();
        });
        builder.setNegativeButton(android.R.string.no, (dialogInterface, i) -> finish());
        alertDialog = builder.show();
        alertDialog.setOnCancelListener(dialog -> finish());
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);

    }


    private void initInstall() {
        source = checkInstallSource();
        boolean needconfirm = sharedPreferences.getBoolean("needconfirm", true);
        show_notification = sharedPreferences.getBoolean("show_notification", false);
        boolean allowsource = sourceSp.getBoolean(source[0], false);
        String apkPath = preInstall();
        cachePath = apkPath;
        if (apkPath == null) {
            showToast0(getString(R.string.failed_prase));
            finish();
        } else {
            if (needconfirm) {
                if (!source[1].equals(ILLEGALPKGNAME) && allowsource) {
                    startInstall(apkPath);
                    finish();
                } else {

                    String[] version = AppInfoUtils.getApplicationVersion(this, apkinfo[1]);

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

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.dialog_install_title));

                    builder.setMessage(alertDialogMessage);
                    View checkBoxView = View.inflate(this, R.layout.confirm_checkbox, null);
                    builder.setView(checkBoxView);
                    CheckBox checkBox = checkBoxView.findViewById(R.id.confirm_checkbox);
                    if (source[1].equals(ILLEGALPKGNAME)) {
                        checkBox.setText(getString(R.string.installsource_unkonwn));
                        checkBox.setEnabled(false);
                    } else {
                        checkBox.setText(String.format(getString(R.string.always_allow), source[1]));
                    }


                    checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            editor = sourceSp.edit();
                            editor.putBoolean(source[0], true);
                            editor.apply();
                        } else {
                            editor = sourceSp.edit();
                            editor.putBoolean(source[0], false);
                            editor.apply();
                        }
                    });
                    builder.setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        cachePath = null;
                        startInstall(apkPath);
                        finish();
                    });
                    builder.setNegativeButton(android.R.string.no, (dialog, which) -> finish());


                    builder.setCancelable(false);
                    alertDialog = builder.show();
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20);
                    alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20);
                }
            } else {
                startInstall(apkPath);
                finish();
            }
        }
    }

    private String reflectGetReferrer() {
        try {
            Class activityClass = Class.forName("android.app.Activity");

            Field refererField = activityClass.getDeclaredField("mReferrer");
            refererField.setAccessible(true);
            return (String) refererField.get(this);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String[] checkInstallSource() {
        final String fromPkgLabel;
        final String fromPkgName;

        String referrer = reflectGetReferrer();
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
                AppInfoUtils.getApplicationLabel(this, fromPkgName);
        if (AppInfoUtils.UNINSTALLED.equals(refererPackageLabel)) {
            fromPkgLabel = ILLEGALPKGNAME;
        } else {
            fromPkgLabel = refererPackageLabel;
        }
        return new String[]{fromPkgName, fromPkgLabel};
    }

    @Override
    public void onResume() {
        super.onResume();
        if (needrequest) {
            if (checkPermission()) {
                initInstall();
            } else {
                requestPermission();
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (istemp && (cachePath != null)) {
            deleteSingleFile(new File(cachePath));
        }
        if (alertDialog != null) {
            alertDialog.dismiss();
        }

    }

    private String preInstall() {
        String apkPath = null;
        if (uri != null) {
            Log.e("--getData--", uri + "");
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                apkPath = uri.getPath();
            } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                apkPath = createApkFromUri(this);
            } else {
                showToast0(getString(R.string.failed_prase));
                finish();
            }
            apkinfo = AppInfoUtils.getApkInfo(this, apkPath);
            if (apkinfo != null) {
                return apkPath;
            } else {

                if (ContentResolver.SCHEME_FILE.equals(uri.getScheme()) && apkPath != null && AppInfoUtils.getApplicationVersion(this, "moe.shizuku.redirectstorage") != null) {
                    return checkSR(apkPath);
                } else {
                    return null;
                }

            }
        } else {
            finish();
            return null;
        }
    }

    private String checkSR(@NonNull String apkPath) {
        String prefix = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (apkPath.startsWith(prefix)) {
            StringBuilder stringBuilder = new StringBuilder(apkPath);
            @SuppressLint("SdCardPath") String toInsert = "/Android/data/" + source[0] + "/sdcard";
            stringBuilder.insert(prefix.length(), toInsert);
            apkPath = stringBuilder.toString();
            Log.e("SRnewpath", apkPath);
            apkinfo = AppInfoUtils.getApkInfo(this, apkPath);
            if (apkinfo != null) {
                return apkPath;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    protected abstract void startInstall(String apkPath);

    protected abstract void startUninstall(String pkgName);

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, permissions, 0x233);
    }

    private boolean checkPermission() {
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return (permissionRead == 0);
    }

    void copyErr(String Err) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, Err);
        Objects.requireNonNull(clipboard).setPrimaryClip(clipData);
    }

    void showToast0(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_SHORT).show());
    }

    void showToast1(final String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
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

    void deleteCache() {
        if (istemp) {
            deleteSingleFile(apkFile);
        }
    }

    private void deleteSingleFile(File file) {
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




}
