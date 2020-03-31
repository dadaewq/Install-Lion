package com.modosa.apkinstaller.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.modosa.apkinstaller.R;
import com.modosa.apkinstaller.util.ResultUtil;
import com.modosa.apkinstaller.util.ShellUtil;
import com.modosa.apkinstaller.util.apksource.ApkSource;
import com.modosa.apkinstaller.util.installer.ApkSourceBuilder;
import com.modosa.apkinstaller.util.installer.SAIPackageInstaller;
import com.modosa.apkinstaller.util.installer.rooted.RootedSAIPackageInstaller;
import com.modosa.apkinstaller.util.shell.Shell;
import com.modosa.apkinstaller.util.shell.SuShell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * @author dadaewq
 */
public class Install4Activity extends AbstractInstallerActivity implements SAIPackageInstaller.InstallationStatusListener {
    public final static String CHANNEL_ID = "4";
    private ArrayList<File> files;
    private long mOngoingSessionId;
    private String installApkPath;
    private String uninstallPkgName;

    @Override
    public void startInstall(String getinstallApkPath) {
        Log.d("Start install", getinstallApkPath + "");
        if (getinstallApkPath != null) {
            installApkPath = getinstallApkPath;
            installApkFile = new File(installApkPath);

            files = new ArrayList<>();
            files.add(installApkFile);

            new InstallApkTask().start();
        }
    }

    @Override
    protected void startUninstall(String getUninstallPkgName) {
        this.uninstallPkgName = getUninstallPkgName;
        new UninstallApkTask().start();
    }


    private void installPackages(List<File> apkFiles) {
        Context mContext = getApplication();
        SAIPackageInstaller mInstaller = RootedSAIPackageInstaller.getInstance(mContext);
        mInstaller.addStatusListener(this);
        ApkSource apkSource = new ApkSourceBuilder()
                .fromApkFiles(apkFiles)
                .build();

        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
    }


    @Override
    public void onStatusChanged(long installationId, SAIPackageInstaller.InstallationStatus status, @Nullable String packageNameOrErrorDescription) {
        if (installationId != mOngoingSessionId) {
            deleteCache();
            return;
        }
        Log.d("status", status + "");

        switch (status) {
            case QUEUED:
            case INSTALLING:
                break;
            case INSTALLATION_SUCCEED:
                showMyToast0(String.format(getString(R.string.tip_success_install), apkinfo[0]));
                showNotificationWithdeleteCache(CHANNEL_ID, true);
                finish();
                break;
            case INSTALLATION_FAILED:

                if (packageNameOrErrorDescription == null) {
                    showNotificationWithdeleteCache(CHANNEL_ID, false);
                    copyErr(getString(R.string.unknown));
                    showMyToast1(String.format(getString(R.string.tip_failed_install_witherror), apkinfo[0], ""));
                } else {
                    if (packageNameOrErrorDescription.contains(getString(R.string.installer_error_root_no_root))) {
                        installByShellUtil();
                    } else {
                        showNotificationWithdeleteCache(CHANNEL_ID, false);
                        copyErr(packageNameOrErrorDescription);
                        String err = packageNameOrErrorDescription.substring(packageNameOrErrorDescription.indexOf("Err:") + 4);
                        showMyToast1(String.format(getString(R.string.tip_failed_install_witherror), apkinfo[0], err));
                    }
                }
                finish();
                break;
            default:
                finish();
        }
    }


    private void installByShellUtil() {
        String installcommand = "pm install -r -d --user 0 -i " + getPackageName() + " \"" + installApkPath + "\"";

        String[] resultselinux = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resultselinux = ShellUtil.execWithRoot("setenforce 0");
        }
        Log.e("installcommand", installcommand);

        String[] result = ShellUtil.execWithRoot(installcommand);


        if ("0".equals(result[3])) {
            showNotificationWithdeleteCache(CHANNEL_ID, true);
            showMyToast0(String.format(getString(R.string.tip_success_install), apkinfo[0]));
        } else {
            showNotificationWithdeleteCache(CHANNEL_ID, false);
            String installerVersion = "???";
            try {
                installerVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException ignore) {
            }
            StringBuilder err = new StringBuilder(String.format("%s: %s %s | %s | Android %s | Install Lion-Root %s\n\n", getString(R.string.installer_device), Build.BRAND, Build.MODEL, ResultUtil.isMiui() ? "MIUI" : " ", Build.VERSION.RELEASE, installerVersion))
                    .append(String.format("Command: %s\nExit code: %s\nOut:\n%s\n=============\nErr:\n%s", result[2], result[3], result[0], result[1]));

            if (resultselinux != null && !"0".equals(resultselinux[3])) {
                copyErr(err.append("\n") + resultselinux[1]);

                int i = 0;
                for (String key : resultselinux
                ) {
                    i++;
                    Log.e(i + " ", key + "");
                }
                showMyToast1(String.format(getString(R.string.tip_failed_install_witherror), apkinfo[0], resultselinux[1] + "\n" + result[1]));
            } else {
                copyErr(err.toString());
                showMyToast1(String.format(getString(R.string.tip_failed_install_witherror), apkinfo[0], result[1]));
            }

        }
    }


    private class InstallApkTask extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            super.run();
            if (SuShell.getInstance().isAvailable()) {
                showMyToast0(String.format(getString(R.string.tip_start_install), apkinfo[0]));
                try {
                    installPackages(files);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                deleteCache();
                showMyToast1(String.format(getString(R.string.tip_failed_install), getString(R.string.installer_error_root_no_root)));
            }
            Looper.loop();
        }
    }

    private class UninstallApkTask extends Thread {
        @Override
        public void run() {
            Looper.prepare();
            super.run();
            Log.d("Start uninstall", uninstallPkgName);

            if (SuShell.getInstance().isAvailable()) {
                Shell.Result uninstallationResult = SuShell.getInstance().exec(new Shell.Command("pm", "uninstall", uninstallPkgName));
                if (0 == uninstallationResult.exitCode) {
                    showMyToast0(String.format(getString(R.string.tip_success_uninstall), uninstallPackageLable));
                } else {
                    String installerVersion = "???";
                    try {
                        installerVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    } catch (PackageManager.NameNotFoundException ignore) {
                    }
                    String info = String.format("%s: %s %s | %s | Android %s | Install Lion-Root %s\n\n", getString(R.string.installer_device), Build.BRAND, Build.MODEL, ResultUtil.isMiui() ? "MIUI" : " ", Build.VERSION.RELEASE, installerVersion);
                    copyErr(info + uninstallationResult.toString());
                    showMyToast1(String.format(getString(R.string.tip_failed_uninstall_witherror), uninstallPackageLable, uninstallationResult.err));
                }
            } else {
                showMyToast1(String.format(getString(R.string.tip_failed_uninstall), getString(R.string.installer_error_root_no_root)));
            }
            Looper.loop();
        }
    }
}
