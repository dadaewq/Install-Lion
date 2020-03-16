package com.modosa.apkinstaller.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.NotifyUtil;
import com.modosa.apkinstaller.util.ResultUtil;
import com.modosa.apkinstaller.util.apksource.ApkSource;
import com.modosa.apkinstaller.util.installer.ApkSourceBuilder;
import com.modosa.apkinstaller.util.installer.SAIPackageInstaller;
import com.modosa.apkinstaller.util.installer.shizuku.ShizukuSAIPackageInstaller;
import com.modosa.apkinstaller.util.shell.Shell;
import com.modosa.apkinstaller.util.shell.ShizukuShell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author dadaewq
 */
public class Install3Activity extends AbstractInstallerActivity implements SAIPackageInstaller.InstallationStatusListener {

    private long mOngoingSessionId;
    private String installApkPath;


    @Override
    public void startInstall(String getinstallApkPath) {
        Log.d("Start install", getinstallApkPath + "");
        if (getinstallApkPath != null) {
            installApkPath = getinstallApkPath;
            installApkFile = new File(installApkPath);
            ArrayList<File> files = new ArrayList<>();
            files.add(installApkFile);
            new Thread(() -> {
                showMyToast0(String.format(getString(R.string.tip_start_install), apkinfo[0]));
                try {
                    installPackages(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finish();
            }

            ).start();
        }
    }


    @Override
    protected void startUninstall(String pkgName) {
        Log.d("Start uninstall", pkgName);
        new Thread(() -> {
            Looper.prepare();
            if (!ShizukuShell.getInstance().isAvailable()) {
                copyErr(String.format("%s\n\n%s\n%s", getString(R.string.title_dialog_uninstall), alertDialogMessage, getString(R.string.installer_error_shizuku_unavailable)));
                showMyToast1(String.format(getString(R.string.tip_failed_uninstall_witherror), uninstallPackageLable, getString(R.string.installer_error_shizuku_unavailable)));
            } else {
                Shell.Result uninstallationResult = ShizukuShell.getInstance().exec(new Shell.Command("pm", "uninstall", pkgName));
                if (0 == uninstallationResult.exitCode) {
                    showMyToast0(String.format(getString(R.string.tip_success_uninstall), uninstallPackageLable));
                } else {
                    String ILVersion = "???";
                    try {
                        ILVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    } catch (PackageManager.NameNotFoundException ignore) {
                    }
                    String info = String.format("%s: %s %s | %s | Android %s | Install Lion %s\n\n", getString(R.string.installer_device), Build.BRAND, Build.MODEL, ResultUtil.isMiui() ? "MIUI" : "Not MIUI", Build.VERSION.RELEASE, ILVersion);
                    copyErr(info + uninstallationResult.toString());
                    showMyToast1(String.format(getString(R.string.tip_failed_uninstall_witherror), uninstallPackageLable, uninstallationResult.err));
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
                showMyToast0(String.format(getString(R.string.tip_success_install), apkinfo[0]));
                showNotificationWithdeleteCache(true);
                break;
            case INSTALLATION_FAILED:

                if (packageNameOrErrorDescription != null) {
                    copyErr(packageNameOrErrorDescription);
                    showMyToast1(String.format(getString(R.string.tip_failed_install_witherror), apkinfo[0], packageNameOrErrorDescription));
                } else {
                    copyErr(getString(R.string.unknown));
                    showMyToast1(String.format(getString(R.string.tip_failed_install_witherror), apkinfo[0], ""));
                }

                showNotificationWithdeleteCache(false);

                break;
            default:
                finish();
        }
    }

    private void showNotificationWithdeleteCache(boolean success) {
        if (success) {
            isInstalledSuccess = true;
            deleteCache();
            if (show_notification) {
                Log.e("packagename", apkinfo[1]);
                new NotifyUtil(this).sendNotification("3", String.format(getString(R.string.tip_success_install), apkinfo[0]), apkinfo[1]);
            }

        } else {
            isInstalledSuccess = false;
            if (show_notification) {
                Log.e("packagename", apkinfo[1]);
                new NotifyUtil(this).sendNotification("21", String.format(getString(R.string.tip_failed_install), apkinfo[0]), apkinfo[1], installApkPath, istemp);
            } else {
                deleteCache();
            }
        }
    }
}
