package com.modosa.apkinstaller.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.utils.Utils;
import com.modosa.apkinstaller.utils.apksource.ApkSource;
import com.modosa.apkinstaller.utils.installer.ApkSourceBuilder;
import com.modosa.apkinstaller.utils.installer.SAIPackageInstaller;
import com.modosa.apkinstaller.utils.installer.shizuku.ShizukuSAIPackageInstaller;
import com.modosa.apkinstaller.utils.shell.Shell;
import com.modosa.apkinstaller.utils.shell.ShizukuShell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author dadaewq
 */
public class Install3Activity extends AbstractInstallActivity implements SAIPackageInstaller.InstallationStatusListener {

    private long mOngoingSessionId;


    @Override
    public void startInstall(String apkPath) {
        Log.d("Start install", apkPath + "");
        if (apkPath != null) {
            apkFile = new File(apkPath);

            ArrayList<File> files = new ArrayList<>();
            files.add(apkFile);
            new Thread(() -> {
                showToast0(String.format(getString(R.string.start_install), apkinfo[0]));
                try {
                    installPackages(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }

            ).start();
        } else {
            showToast0(getString(R.string.failed_read));
            finish();
        }
    }


    @Override
    protected void startUninstall(String pkgname) {
        Log.d("Start uninstall", pkgname);
        new Thread(() -> {
            Looper.prepare();
            if (!ShizukuShell.getInstance().isAvailable()) {
                copyErr(String.format("%s\n\n%s\n%s", getString(R.string.dialog_uninstall_title), alertDialogMessage, getString(R.string.installer_error_shizuku_unavailable)));
                showToast1(String.format(getString(R.string.failed_uninstall), packageLable, getString(R.string.installer_error_shizuku_unavailable)));
            } else {
                Shell.Result uninstallationResult = ShizukuShell.getInstance().exec(new Shell.Command("pm", "uninstall", pkgname));
                if (0 == uninstallationResult.exitCode) {
                    showToast0(String.format(getString(R.string.success_uninstall), packageLable));
                } else {
                    String ILVersion = "???";
                    try {
                        ILVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    } catch (PackageManager.NameNotFoundException ignore) {
                    }
                    String info = String.format("%s: %s %s | %s | Android %s | Install Lion %s\n\n", getString(R.string.installer_device), Build.BRAND, Build.MODEL, Utils.isMiui() ? "MIUI" : "Not MIUI", Build.VERSION.RELEASE, ILVersion);
                    copyErr(info + uninstallationResult.toString());
                    showToast1(String.format(getString(R.string.failed_uninstall), packageLable, uninstallationResult.err));
                }
            }
            Looper.loop();
        }
        ).start();
        finish();
    }

    private void installPackages(List<File> apkFiles) {
        Context mContext = getApplication();
        SAIPackageInstaller mInstaller = ShizukuSAIPackageInstaller.getInstance(mContext);
        mInstaller.addStatusListener(this);
        ApkSource apkSource = new ApkSourceBuilder()
                .fromApkFiles(apkFiles)
                .build();
        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
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
                deleteCache();
                showToast0(String.format(getString(R.string.success_install), apkinfo[0]));
                if (show_notification) {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(getPackageName(), getPackageName() + ".activity.NotifyActivity"));
                    Log.e("packagename", apkinfo[1]);

                    intent.putExtra("channelId", "3");
                    intent.putExtra("channelName", getString(R.string.name_install3));
                    intent.putExtra("packageName", apkinfo[1]);
                    intent.putExtra("packageLable", apkinfo[0]);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                break;
            case INSTALLATION_FAILED:
                deleteCache();
                if (packageNameOrErrorDescription != null) {
                    copyErr(packageNameOrErrorDescription);
                    showToast1(String.format(getString(R.string.failed_install), apkinfo[0], packageNameOrErrorDescription));
                } else {
                    copyErr(getString(R.string.unknown));
                    showToast1(String.format(getString(R.string.failed_install), apkinfo[0], ""));
                }
                break;
            default:
                finish();
        }
    }

}
